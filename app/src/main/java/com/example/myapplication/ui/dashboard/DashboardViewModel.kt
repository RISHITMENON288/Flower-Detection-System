package com.example.myapplication.ui.dashboard

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.github.DashboardData
import com.example.myapplication.data.github.DashboardRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val data: DashboardData? = null
)

/**
 * Loads the SCM dashboard from GitHub. Created once in MainScaffold, so
 * opening and closing the Profile screen does not re-download everything
 * (GitHub allows about 60 requests an hour without login). The refresh
 * button forces a reload.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var started = false

    /** Loads on first call only; later calls do nothing. */
    fun loadOnce() {
        if (started) return
        started = true
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val (name, code) = installedVersion()
            val data = try {
                DashboardRepository.load(name, code)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            // Keep the previous data if a refresh fails outright.
            _uiState.update { DashboardUiState(isLoading = false, data = data ?: it.data) }
        }
    }

    private fun installedVersion(): Pair<String, Long> = try {
        val app = getApplication<Application>()
        val info = app.packageManager.getPackageInfo(app.packageName, 0)
        (info.versionName ?: "unknown") to PackageInfoCompat.getLongVersionCode(info)
    } catch (e: Exception) {
        "unknown" to 0L
    }
}
