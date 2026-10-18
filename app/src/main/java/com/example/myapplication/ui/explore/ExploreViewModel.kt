package com.example.myapplication.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.FlowerLabels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Holds the search query and filtered list. The underlying data
 * (FlowerLabels) is static, but Explore still follows the same
 * UI-only-talks-to-a-ViewModel pattern as Home and History.
 */
class ExploreViewModel : ViewModel() {
    private val allNames = FlowerLabels.NAMES.sorted()
    val totalCount: Int = allNames.size

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val filteredNames: StateFlow<List<String>> = _query
        .map { q -> if (q.isBlank()) allNames else allNames.filter { it.contains(q, ignoreCase = true) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), allNames)

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }
}
