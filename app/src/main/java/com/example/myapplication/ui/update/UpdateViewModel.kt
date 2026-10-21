package com.example.myapplication.ui.update

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.ReleaseInfo
import com.example.myapplication.data.UpdateChecker
import com.example.myapplication.data.update.UpdateManager
import com.example.myapplication.data.update.UpdatePlan
import com.example.myapplication.data.update.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Checks GitHub once per app session for a newer release than what's
 * installed. When a new version is detected, evaluates whether a bandwidth-efficient
 * incremental (delta) patch is available, or automatically prepares a full-package fallback.
 *
 * Provides real-time state for downloading, integrity verification, binary patching,
 * and launching the Android system package installer.
 */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val updateManager by lazy { UpdateManager(application) }

    private val _availableUpdate = MutableStateFlow<ReleaseInfo?>(null)
    val availableUpdate: StateFlow<ReleaseInfo?> = _availableUpdate.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var hasChecked = false

    /**
     * Checks for updates once per session. In debug builds, by default it doesn't prompt,
     * but [forceCheck] allows manual verification from the SCM dashboard or testing.
     */
    fun checkForUpdateOnce(forceCheck: Boolean = false) {
        if (hasChecked && !forceCheck) return
        hasChecked = true

        _updateState.value = UpdateState.Checking

        viewModelScope.launch {
            val app = getApplication<Application>()
            val currentVersion = currentVersionNameOrNull() ?: return@launch
            val release = withContext(Dispatchers.IO) {
                UpdateChecker.fetchLatestRelease(app)
            }

            if (release != null && UpdateChecker.isNewerVersion(release.versionTag, currentVersion)) {
                _availableUpdate.value = release
                if (release.plan != null) {
                    _updateState.value = UpdateState.Available(release.plan)
                } else {
                    _updateState.value = UpdateState.Idle
                }
            } else {
                _updateState.value = UpdateState.Idle
            }
        }
    }

    /**
     * Begins downloading and applying the update based on the current plan.
     */
    fun startUpdate() {
        val plan = (_updateState.value as? UpdateState.Available)?.plan
            ?: _availableUpdate.value?.plan
            ?: return

        viewModelScope.launch {
            updateManager.executeUpdate(plan) { state ->
                _updateState.value = state
                if (state is UpdateState.ReadyToInstall) {
                    // Automatically prompt the package installer when ready
                    updateManager.installApk(state.apkFile)
                }
            }
        }
    }

    /**
     * Re-triggers installation for an already downloaded/patched APK.
     */
    fun installPendingUpdate() {
        val ready = _updateState.value as? UpdateState.ReadyToInstall ?: return
        updateManager.installApk(ready.apkFile)
    }

    /**
     * Dismisses the update banner/overlay.
     */
    fun dismiss() {
        _availableUpdate.value = null
        _updateState.value = UpdateState.Idle
    }

    /**
     * Preview helper for testing the glass update dialog on demand,
     * even when the installed version already matches GitHub.
     */
    fun previewUpdateOverlay() {
        val app = getApplication<Application>()
        val current = currentVersionNameOrNull() ?: "5.1"
        val mockPlan = com.example.myapplication.data.update.UpdatePlan.Delta(
            patch = com.example.myapplication.data.update.DeltaPatchAsset(
                fromVersionName = current,
                fromVersionCode = 5,
                patchFileName = "patch_v${current}_to_v5.2.patch",
                downloadUrl = "https://github.com/RISHITMENON288/Flower-Detection-System/releases/download/v5.1/app-debug.apk",
                patchSizeBytes = 2097152L,
                patchSha256 = null,
                targetSha256 = null
            ),
            manifest = com.example.myapplication.data.update.UpdateManifest(
                versionName = "5.2",
                versionCode = 6,
                releaseTag = "v5.2",
                releaseUrl = "https://github.com/RISHITMENON288/Flower-Detection-System/releases/tag/v5.1",
                changelog = "Delta update demonstration with bandwidth savings.",
                fullApk = com.example.myapplication.data.update.FullApkAsset(
                    fileName = "app-release.apk",
                    downloadUrl = "https://github.com/RISHITMENON288/Flower-Detection-System/releases/download/v5.1/app-debug.apk",
                    sizeBytes = 52428800L
                ),
                patches = emptyList()
            ),
            sourceApk = java.io.File(app.applicationInfo.sourceDir),
            savingsPercent = 96
        )

        val release = ReleaseInfo(
            versionTag = "v5.2",
            releaseUrl = "https://github.com/RISHITMENON288/Flower-Detection-System/releases/tag/v5.1",
            releaseNotes = "• Faster flower classification\n• SCM dashboard metrics pills\n• Bandwidth-efficient incremental updates",
            plan = mockPlan
        )
        _availableUpdate.value = release
        _updateState.value = UpdateState.Available(mockPlan)
    }

    private fun currentVersionNameOrNull(): String? = try {
        val app = getApplication<Application>()
        app.packageManager.getPackageInfo(app.packageName, 0).versionName
    } catch (e: Exception) {
        null
    }
}
