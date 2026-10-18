package com.example.myapplication.data

import android.graphics.Bitmap
import com.example.myapplication.ml.Prediction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HistoryEntry(
    val id: Long,
    val thumbnail: Bitmap,
    val topPrediction: Prediction,
    val timestampMillis: Long
)

/**
 * In-memory log of past identifications for this app session. Resets when
 * the app process dies — swap this for a Room database later if you want
 * history to survive restarts; HistoryViewModel is the only thing that
 * touches this, so nothing in the UI layer would need to change.
 */
object HistoryRepository {
    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    fun add(thumbnail: Bitmap, topPrediction: Prediction) {
        val entry = HistoryEntry(
            id = System.currentTimeMillis(),
            thumbnail = thumbnail,
            topPrediction = topPrediction,
            timestampMillis = System.currentTimeMillis()
        )
        _entries.value = listOf(entry) + _entries.value
    }
}
