package com.example.nammapustaka.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nammapustaka.data.LibraryRepository
import com.example.nammapustaka.data.local.BookCatalogRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class CatalogViewModel(
    private val repo: LibraryRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow<String?>(null)

    val query: StateFlow<String> = _query.asStateFlow()
    val category: StateFlow<String?> = _category.asStateFlow()

    val books: StateFlow<List<BookCatalogRow>> =
        combine(_query, _category) { q, c -> q to c }
            .flatMapLatest { (q, c) -> repo.observeCatalog(q, c) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setCategory(value: String?) {
        _category.value = value
    }

    companion object {
        fun factory(repo: LibraryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(CatalogViewModel::class.java))
                    return CatalogViewModel(repo) as T
                }
            }
    }
}
