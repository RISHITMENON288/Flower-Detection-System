package com.example.myapplication.data

import android.content.Context
import android.os.Build
import com.example.myapplication.data.update.DeltaPatchAsset
import com.example.myapplication.data.update.FullApkAsset
import com.example.myapplication.data.update.IntegrityVerifier
import com.example.myapplication.data.update.UpdateManifest
import com.example.myapplication.data.update.UpdatePlan
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * A GitHub release newer than what's currently installed, with an optional
 * delta-or-full [plan] describing how to download and apply it.
 */
data class ReleaseInfo(
    val versionTag: String,
    val releaseUrl: String,
    val releaseNotes: String? = null,
    val plan: UpdatePlan? = null
)

/**
 * Checks the project's public GitHub repo for a newer release by hitting
 * GitHub's public Releases API directly. No auth token needed (the repo
 * is public), and no new HTTP dependency: HttpURLConnection + org.json
 * are both already part of the Android SDK.
 *
 * Automatically detects whether an incremental (delta) patch is available
 * for the currently installed version or falls back to the full APK package.
 *
 * This does blocking network I/O — every call into this object must run
 * on Dispatchers.IO. See UpdateViewModel, the only caller.
 */
object UpdateChecker {
    // Not private: the dashboard (data/github) reads the same repo.
    const val OWNER = "RISHITMENON288"
    const val REPO = "Flower-Detection-System"

    private const val TIMEOUT_MS = 8000
    private const val MANIFEST_FILE_NAME = "update-manifest.json"
    private const val ALT_MANIFEST_FILE_NAME = "update-metadata.json"

    /**
     * Returns the latest published release, or null on any failure — no
     * network, no releases published yet, a malformed response, GitHub
     * rate-limiting, etc. A failed check never surfaces as an error to
     * the user; it just means no update banner shows this session.
     *
     * If [context] is provided, also analyzes the installed APK and available
     * release assets to assemble an optimized [UpdatePlan.Delta] or [UpdatePlan.Full].
     */
    fun fetchLatestRelease(context: Context? = null): ReleaseInfo? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "FloraScan-Android-App")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val versionTag = json.getString("tag_name")
            val releaseUrl = json.getString("html_url")
            val releaseNotes = json.optString("body", "")
            val assetsArray = json.optJSONArray("assets") ?: JSONArray()

            // Resolve manifest and update plan
            val plan = if (context != null) {
                resolveUpdatePlan(context, versionTag, releaseUrl, releaseNotes, assetsArray)
            } else {
                null
            }

            ReleaseInfo(
                versionTag = versionTag,
                releaseUrl = releaseUrl,
                releaseNotes = releaseNotes,
                plan = plan
            )
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Analyzes release assets and current installation to generate an UpdatePlan.
     */
    private fun resolveUpdatePlan(
        context: Context,
        releaseTag: String,
        releaseUrl: String,
        releaseNotes: String,
        assetsArray: JSONArray
    ): UpdatePlan? {
        val currentPackageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: Exception) {
            null
        } ?: return null

        val currentVersionName = currentPackageInfo.versionName ?: ""
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            (currentPackageInfo.longVersionCode and 0xFFFFFFFFL).toInt()
        } else {
            @Suppress("DEPRECATION")
            currentPackageInfo.versionCode
        }

        val sourceApk = File(context.applicationInfo.sourceDir)

        // 1. Look for update-manifest.json in the release assets
        var manifestDownloadUrl: String? = null
        var fullApkDownloadUrl: String? = null
        var fullApkFileName: String = "app-release.apk"
        var fullApkSize: Long = 0L

        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.getJSONObject(i)
            val name = asset.getString("name")
            val downloadUrl = asset.getString("browser_download_url")
            val size = asset.optLong("size", 0L)

            if (name.equals(MANIFEST_FILE_NAME, ignoreCase = true) ||
                name.equals(ALT_MANIFEST_FILE_NAME, ignoreCase = true)
            ) {
                manifestDownloadUrl = downloadUrl
            } else if (name.endsWith(".apk", ignoreCase = true)) {
                fullApkDownloadUrl = downloadUrl
                fullApkFileName = name
                fullApkSize = size
            }
        }

        // Try downloading and parsing the explicit manifest if published
        var manifest: UpdateManifest? = null
        if (manifestDownloadUrl != null) {
            manifest = fetchManifest(manifestDownloadUrl)
        }

        // If no explicit manifest was attached, synthesize one from the release assets
        if (manifest == null) {
            if (fullApkDownloadUrl == null) {
                // No APK asset attached to this release
                return null
            }
            manifest = UpdateManifest(
                versionName = releaseTag.removePrefix("v").trim(),
                versionCode = 0,
                releaseTag = releaseTag,
                releaseUrl = releaseUrl,
                changelog = releaseNotes,
                fullApk = FullApkAsset(
                    fileName = fullApkFileName,
                    downloadUrl = fullApkDownloadUrl,
                    sizeBytes = fullApkSize
                ),
                patches = emptyList()
            )
        }

        // Determine whether a compatible delta patch exists
        if (sourceApk.exists() && sourceApk.canRead()) {
            val matchingPatch = manifest.patches.firstOrNull { patch ->
                patch.fromVersionName.equals(currentVersionName, ignoreCase = true) ||
                        (patch.fromVersionCode > 0 && patch.fromVersionCode == currentVersionCode)
            }

            if (matchingPatch != null) {
                val savings = IntegrityVerifier.calculateSavingsPercent(
                    matchingPatch.patchSizeBytes,
                    manifest.fullApk.sizeBytes
                )
                return UpdatePlan.Delta(
                    patch = matchingPatch,
                    manifest = manifest,
                    sourceApk = sourceApk,
                    savingsPercent = savings
                )
            }
        }

        // Fallback: full APK download
        return UpdatePlan.Full(
            manifest = manifest,
            reason = "No compatible delta patch found for installed version $currentVersionName"
        )
    }

    /**
     * Downloads and parses update-manifest.json directly.
     */
    private fun fetchManifest(urlStr: String): UpdateManifest? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", "FloraScan-Android-App")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            parseManifestJson(text)
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Parses update-manifest.json content into an [UpdateManifest] instance.
     */
    fun parseManifestJson(jsonString: String): UpdateManifest {
        val root = JSONObject(jsonString)
        val versionName = root.getString("versionName")
        val versionCode = root.optInt("versionCode", 0)
        val releaseTag = root.optString("releaseTag", "v$versionName")
        val releaseUrl = root.optString("releaseUrl", "")
        val changelog = root.optString("changelog", "")

        val fullApkObj = root.getJSONObject("fullApk")
        val fullApk = FullApkAsset(
            fileName = fullApkObj.optString("fileName", "app-release.apk"),
            downloadUrl = fullApkObj.getString("downloadUrl"),
            sizeBytes = fullApkObj.getLong("sizeBytes"),
            sha256 = fullApkObj.optString("sha256").takeIf { it.isNotBlank() }
        )

        val patchesList = mutableListOf<DeltaPatchAsset>()
        val patchesArray = root.optJSONArray("patches")
        if (patchesArray != null) {
            for (i in 0 until patchesArray.length()) {
                val p = patchesArray.getJSONObject(i)
                patchesList.add(
                    DeltaPatchAsset(
                        fromVersionName = p.getString("fromVersionName"),
                        fromVersionCode = p.optInt("fromVersionCode", 0),
                        patchFileName = p.getString("patchFileName"),
                        downloadUrl = p.getString("downloadUrl"),
                        patchSizeBytes = p.getLong("patchSizeBytes"),
                        patchSha256 = p.optString("patchSha256").takeIf { it.isNotBlank() },
                        targetSha256 = p.optString("targetSha256").takeIf { it.isNotBlank() }
                    )
                )
            }
        }

        return UpdateManifest(
            versionName = versionName,
            versionCode = versionCode,
            releaseTag = releaseTag,
            releaseUrl = releaseUrl,
            changelog = changelog,
            fullApk = fullApk,
            patches = patchesList
        )
    }

    /**
     * True if [latestTag] (e.g. "v1.2.0" or "1.2.0") names a different
     * version than [currentVersionName] (e.g. "1.0", read from the
     * installed package at runtime).
     */
    fun isNewerVersion(latestTag: String, currentVersionName: String): Boolean {
        val normalizedLatest = latestTag.removePrefix("v").trim()
        val normalizedCurrent = currentVersionName.removePrefix("v").trim()
        return normalizedLatest.isNotEmpty() && normalizedLatest != normalizedCurrent
    }
}
