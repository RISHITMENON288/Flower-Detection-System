package com.example.myapplication.ui.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.ReleaseInfo
import com.example.myapplication.data.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Checks GitHub once per app session for a newer release than what's
 * installed. This is low-priority background work — it must never delay
 * anything the user is actually doing — so the network call runs on
 * Dispatchers.IO and only ever writes a result when it's done; nothing
 * else in the app waits on it.
 */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val _availableUpdate = MutableStateFlow<ReleaseInfo?>(null)
    val availableUpdate: StateFlow<ReleaseInfo?> = _availableUpdate.asStateFlow()

    private var hasChecked = false

    /** Safe to call on every recomposition/navigation — only the first
     * call per ViewModel instance (i.e. per app session) actually does
     * anything. */
    fun checkForUpdateOnce() {
        if (hasChecked) return
        hasChecked = true

        // UpdateChecker.isNewerVersion() only tests "different from what's
        // installed", not "older" (see its own doc comment) — that's fine
        // for a real install, which is always behind or equal to the
        // latest published release, but a debug build run straight from
        // Android Studio is routinely AHEAD of the last GitHub release you
        // bothered to publish. Without this check that mismatch reads as
        // "update available" every single run, no matter what versionName
        // you set locally — only a real release build should ever ask.
        if (BuildConfig.DEBUG) return

        viewModelScope.launch {
            val currentVersion = currentVersionNameOrNull() ?: return@launch
            val release = withContext(Dispatchers.IO) { UpdateChecker.fetchLatestRelease() }
            if (release != null && UpdateChecker.isNewerVersion(release.versionTag, currentVersion)) {
                _availableUpdate.value = release
            }
        }
    }

    fun dismiss() {
        _availableUpdate.value = null
    }

    private fun currentVersionNameOrNull(): String? = try {
        val app = getApplication<Application>()
        app.packageManager.getPackageInfo(app.packageName, 0).versionName
    } catch (e: Exception) {
        null
    }
}
