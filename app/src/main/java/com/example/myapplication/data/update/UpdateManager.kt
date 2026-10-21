package com.example.myapplication.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Executes the complete update lifecycle: downloading delta patches or full APKs,
 * verifying cryptographic integrity, executing binary patching, and invoking
 * Android's package installer with automatic full-download fallback.
 */
class UpdateManager(private val context: Context) {

    private val updateDir: File by lazy {
        File(context.cacheDir, "updates").apply { mkdirs() }
    }

    /**
     * Executes the update according to [plan].
     * Invokes [onStateChange] as the update progresses through downloading,
     * verification, patching, and completion.
     */
    suspend fun executeUpdate(
        plan: UpdatePlan,
        onStateChange: (UpdateState) -> Unit
    ) = withContext(Dispatchers.IO) {
        cleanOldUpdates()

        when (plan) {
            is UpdatePlan.Delta -> {
                executeDeltaUpdateWithFallback(plan, onStateChange)
            }
            is UpdatePlan.Full -> {
                executeFullUpdate(plan.manifest.fullApk, onStateChange)
            }
        }
    }

    /**
     * Attempts to download and apply a delta patch. If any step fails (download error,
     * corrupted patch, checksum mismatch, or patching error), automatically transitions
     * to downloading the full release package.
     */
    private suspend fun executeDeltaUpdateWithFallback(
        plan: UpdatePlan.Delta,
        onStateChange: (UpdateState) -> Unit
    ) {
        val patch = plan.patch
        val patchFile = File(updateDir, "patch_${patch.fromVersionName}_to_${plan.manifest.versionName}.tmp")
        val targetApk = File(updateDir, "FloraScan-${plan.manifest.versionName}.apk")
        val targetApkTmp = File(updateDir, "FloraScan-${plan.manifest.versionName}.tmp")

        try {
            // 1. Download patch
            onStateChange(
                UpdateState.Downloading(
                    bytesDownloaded = 0L,
                    totalBytes = patch.patchSizeBytes,
                    progressPercent = 0,
                    isDelta = true,
                    statusMessage = "Downloading delta patch (${IntegrityVerifier.formatBytes(patch.patchSizeBytes)})..."
                )
            )

            downloadFile(patch.downloadUrl, patchFile, patch.patchSizeBytes) { downloaded, total, percent ->
                onStateChange(
                    UpdateState.Downloading(
                        bytesDownloaded = downloaded,
                        totalBytes = total,
                        progressPercent = percent,
                        isDelta = true,
                        statusMessage = "Downloading patch: ${IntegrityVerifier.formatBytes(downloaded)} / ${IntegrityVerifier.formatBytes(total)} ($percent%)"
                    )
                )
            }

            // 2. Verify patch checksum
            onStateChange(UpdateState.Verifying("Verifying delta patch integrity (SHA-256)..."))
            if (!patch.patchSha256.isNullOrBlank()) {
                if (!IntegrityVerifier.verifySha256(patchFile, patch.patchSha256)) {
                    throw IOException("Patch SHA-256 verification failed (expected ${patch.patchSha256})")
                }
            }

            // 3. Apply patch
            onStateChange(UpdateState.Patching(0, "Applying delta patch to installed app..."))
            BsPatch.applyPatch(
                sourceFile = plan.sourceApk,
                targetFile = targetApkTmp,
                patchFile = patchFile
            ) { progress ->
                val pct = (progress * 100).toInt()
                onStateChange(UpdateState.Patching(pct, "Applying delta patch ($pct%)..."))
            }

            // 4. Verify target APK checksum
            onStateChange(UpdateState.Verifying("Verifying updated package integrity..."))
            val expectedTargetHash = patch.targetSha256 ?: plan.manifest.fullApk.sha256
            if (!expectedTargetHash.isNullOrBlank()) {
                if (!IntegrityVerifier.verifySha256(targetApkTmp, expectedTargetHash)) {
                    throw IOException("Reconstructed APK SHA-256 mismatch (expected $expectedTargetHash)")
                }
            }

            // 5. Commit target file
            if (targetApk.exists()) targetApk.delete()
            if (!targetApkTmp.renameTo(targetApk)) {
                throw IOException("Failed to rename temporary APK to ${targetApk.name}")
            }

            // Cleanup patch file
            patchFile.delete()

            onStateChange(UpdateState.ReadyToInstall(targetApk, isDeltaApplied = true))

        } catch (e: Exception) {
            // Delta update failed — clean up and automatically trigger full-download fallback
            patchFile.delete()
            targetApkTmp.delete()

            onStateChange(
                UpdateState.Downloading(
                    bytesDownloaded = 0L,
                    totalBytes = plan.manifest.fullApk.sizeBytes,
                    progressPercent = 0,
                    isDelta = false,
                    statusMessage = "Delta patch failed (${e.message}). Falling back to full package..."
                )
            )

            // Execute full package fallback
            try {
                executeFullUpdate(plan.manifest.fullApk, onStateChange)
            } catch (fallbackError: Exception) {
                onStateChange(
                    UpdateState.Failed(
                        errorMessage = "Update failed: ${fallbackError.message}",
                        canFallbackToFull = false
                    )
                )
            }
        }
    }

    /**
     * Downloads and verifies the complete APK package directly.
     */
    private suspend fun executeFullUpdate(
        fullApk: FullApkAsset,
        onStateChange: (UpdateState) -> Unit
    ) {
        val targetApk = File(updateDir, fullApk.fileName)
        val targetApkTmp = File(updateDir, "${fullApk.fileName}.tmp")

        try {
            onStateChange(
                UpdateState.Downloading(
                    bytesDownloaded = 0L,
                    totalBytes = fullApk.sizeBytes,
                    progressPercent = 0,
                    isDelta = false,
                    statusMessage = "Downloading full package (${IntegrityVerifier.formatBytes(fullApk.sizeBytes)})..."
                )
            )

            downloadFile(fullApk.downloadUrl, targetApkTmp, fullApk.sizeBytes) { downloaded, total, percent ->
                onStateChange(
                    UpdateState.Downloading(
                        bytesDownloaded = downloaded,
                        totalBytes = total,
                        progressPercent = percent,
                        isDelta = false,
                        statusMessage = "Downloading package: ${IntegrityVerifier.formatBytes(downloaded)} / ${IntegrityVerifier.formatBytes(total)} ($percent%)"
                    )
                )
            }

            // Verify full APK SHA-256
            onStateChange(UpdateState.Verifying("Verifying package checksum (SHA-256)..."))
            if (!fullApk.sha256.isNullOrBlank()) {
                if (!IntegrityVerifier.verifySha256(targetApkTmp, fullApk.sha256)) {
                    throw IOException("Full APK SHA-256 mismatch (expected ${fullApk.sha256})")
                }
            }

            if (targetApk.exists()) targetApk.delete()
            if (!targetApkTmp.renameTo(targetApk)) {
                throw IOException("Failed to finalize downloaded APK")
            }

            onStateChange(UpdateState.ReadyToInstall(targetApk, isDeltaApplied = false))

        } catch (e: Exception) {
            targetApkTmp.delete()
            onStateChange(
                UpdateState.Failed(
                    errorMessage = "Failed to download update: ${e.message}",
                    canFallbackToFull = false
                )
            )
        }
    }

    /**
     * Downloads a file from [urlStr] to [destinationFile] reporting progress.
     */
    @Throws(IOException::class)
    private fun downloadFile(
        urlStr: String,
        destinationFile: File,
        expectedLength: Long,
        onProgress: (bytesRead: Long, totalBytes: Long, percent: Int) -> Unit
    ) {
        var connection: HttpURLConnection? = null
        try {
            var url = URL(urlStr)
            var redirects = 0
            while (redirects < 5) {
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "FloraScan-Android-App")
                }
                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                    val loc = connection.getHeaderField("Location") ?: break
                    url = URL(loc)
                    connection.disconnect()
                    redirects++
                } else {
                    break
                }
            }

            val responseCode = connection?.responseCode ?: -1
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP $responseCode")
            }

            val totalBytes = if (expectedLength > 0) expectedLength else connection!!.contentLength.toLong()
            destinationFile.parentFile?.mkdirs()

            BufferedInputStream(connection!!.inputStream).use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val pct = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                        onProgress(totalRead, totalBytes, pct)
                    }
                    output.flush()
                }
            }
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Launches Android's PackageInstaller to install the given [apkFile].
     * Checks unknown sources permission on Android 8.0+.
     */
    fun installApk(apkFile: File) {
        if (!apkFile.exists()) return

        // On Android 8.0+, check if the app has permission to request package installs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        val authority = "${context.packageName}.fileprovider"
        val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }

    /**
     * Cleans up temporary files in the update cache directory.
     */
    fun cleanOldUpdates() {
        try {
            updateDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".tmp")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}
