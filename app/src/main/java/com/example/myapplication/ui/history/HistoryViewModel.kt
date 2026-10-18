package com.example.myapplication.ui.history

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.HistoryEntry
import com.example.myapplication.data.HistoryRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin wrapper so HistoryScreen only ever talks to a ViewModel, never the
 * repository directly — keeps the same access pattern as HomeViewModel.
 */
class HistoryViewModel : ViewModel() {
    val entries: StateFlow<List<HistoryEntry>> = HistoryRepository.entries
}
