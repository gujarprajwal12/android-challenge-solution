package com.prajwal.guajr.randomstringgenerator.ViewModel

import androidx.lifecycle.ViewModel
import com.prajwal.guajr.randomstringgenerator.DataClass.RandomString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class RandomStringViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val strings: List<RandomString> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    fun addString(randomString: RandomString) {
        _uiState.update { it.copy(strings = listOf(randomString) + it.strings) }
    }

    fun removeString(randomString: RandomString) {
        _uiState.update { it.copy(strings = it.strings.toMutableList().apply { remove(randomString) }) }
    }

    fun clearAllStrings() {
        _uiState.update { it.copy(strings = emptyList()) }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(error = message, isLoading = false) }
    }
}