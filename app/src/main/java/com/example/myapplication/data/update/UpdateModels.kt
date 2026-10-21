package com.example.myapplication.data.update

import java.io.File

/**
 * Metadata for a full APK package published on GitHub Releases.
 */
data class FullApkAsset(
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val sha256: String? = null
)

/**
 * Metadata for a delta patch asset published on GitHub Releases.
 * Applies to an existing installed version [fromVersionName] (or [fromVersionCode])
 * to produce the target release APK.
 */
data class DeltaPatchAsset(
    val fromVersionName: String,
    val fromVersionCode: Int = 0,
    val patchFileName: String,
    val downloadUrl: String,
    val patchSizeBytes: Long,
    val patchSha256: String? = null,
    val targetSha256: String? = null
)

/**
 * Complete release manifest published as update-manifest.json or synthesized
 * from GitHub Release assets.
 */
data class UpdateManifest(
    val versionName: String,
    val versionCode: Int,
    val releaseTag: String,
    val releaseUrl: String,
    val changelog: String = "",
    val fullApk: FullApkAsset,
    val patches: List<DeltaPatchAsset> = emptyList()
)

/**
 * Strategy for updating the current installation to the target release.
 */
sealed class UpdatePlan {
    /**
     * An incremental update: downloads only the binary delta patch and applies it
     * against the currently installed APK ([sourceApk]).
     */
    data class Delta(
        val patch: DeltaPatchAsset,
        val manifest: UpdateManifest,
        val sourceApk: File,
        val savingsPercent: Int
    ) : UpdatePlan()

    /**
     * Full package download fallback: used when no patch is available for the current
     * version, or when a delta patch fails validation/application.
     */
    data class Full(
        val manifest: UpdateManifest,
        val reason: String
    ) : UpdatePlan()
}

/**
 * Execution state of the in-app update process.
 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Available(val plan: UpdatePlan) : UpdateState()
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progressPercent: Int,
        val isDelta: Boolean,
        val statusMessage: String
    ) : UpdateState()
    data class Verifying(val message: String) : UpdateState()
    data class Patching(val progressPercent: Int, val message: String) : UpdateState()
    data class ReadyToInstall(val apkFile: File, val isDeltaApplied: Boolean) : UpdateState()
    object Installing : UpdateState()
    data class Failed(val errorMessage: String, val canFallbackToFull: Boolean) : UpdateState()
}
