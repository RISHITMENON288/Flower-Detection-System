package com.example.myapplication.ui.home

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.HistoryRepository
import com.example.myapplication.ml.FlowerClassifier
import com.example.myapplication.ml.Prediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val selectedImage: Bitmap? = null,
    val isClassifying: Boolean = false,
    val predictions: List<Prediction> = emptyList(),
    val error: String? = null
)

/**
 * Owns the model. HomeScreen only reads [uiState] and calls [onImageSelected] —
 * it never imports or touches FlowerClassifier directly.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // Exposed as a Lazy so onCleared() can check isInitialized() without
    // forcing a load just to decide whether there's anything to release.
    private val classifierLazy = lazy { FlowerClassifier(application) }
    private val classifier by classifierLazy

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Tracks the in-flight classification so a second image pick (camera
    // retake, rapid gallery taps) cancels the stale one instead of letting
    // two results race to write _uiState — without this, an old, slower
    // classification finishing after a newer one could overwrite the
    // correct result with a stale one.
    private var classificationJob: Job? = null

    fun onImageSelected(bitmap: Bitmap) {
        classificationJob?.cancel()
        _uiState.value = HomeUiState(selectedImage = bitmap, isClassifying = true)
        classificationJob = viewModelScope.launch {
            try {
                // First touch of `classifier` triggers FlowerClassifier's
                // constructor, which copies the model asset to internal
                // storage (first run only) and parses it natively — that's
                // I/O-shaped setup work, not the CPU-bound inference below,
                // so it runs on Dispatchers.IO. `by lazy` is synchronized,
                // so later calls just read the cached instance here almost
                // instantly.
                val loadedClassifier = withContext(Dispatchers.IO) { classifier }
                val results = withContext(Dispatchers.Default) {
                    loadedClassifier.classify(bitmap, topK = 5)
                }
                _uiState.value = _uiState.value.copy(isClassifying = false, predictions = results)
                results.firstOrNull()?.let { top ->
                    HistoryRepository.add(thumbnail = bitmap, topPrediction = top)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isClassifying = false,
                    error = e.message ?: "Couldn't identify this image"
                )
            }
        }
    }

    fun clearSelection() {
        _uiState.value = HomeUiState()
    }

    /** Releases the native PyTorch module deterministically once this
     * screen's ViewModel is torn down, instead of relying on GC to ever
     * collect (and finalize) the native handle. Only touches it if a
     * classification actually ran — never forces a load just to free it. */
    override fun onCleared() {
        classificationJob?.cancel()
        if (classifierLazy.isInitialized()) {
            classifierLazy.value.close()
        }
    }
}
