package com.example.myapplication7.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication7.data.*
import com.example.myapplication7.data.local.AppDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ЛР №9: Стани UI екрану списку
sealed class ConcertListUiState {
    object Loading : ConcertListUiState()
    data class Success(
        val concerts: List<Concert>,
        val isOffline: Boolean = false  // Завдання 5: індикатор офлайн-режиму
    ) : ConcertListUiState()
    data class Error(val message: String) : ConcertListUiState()
    object Empty : ConcertListUiState()
}

// ЛР №9: Стани екрану деталей
sealed class ConcertDetailUiState {
    object Loading : ConcertDetailUiState()
    data class Success(val concert: Concert) : ConcertDetailUiState()
    data class Error(val message: String) : ConcertDetailUiState()
}

// ЛР №9 Завдання 4+5: Одноразові події (Snackbar, Toast)
sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    object NavigateBack : UiEvent()
}

class BeatStreamViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db = db)

    // --- Список концертів ---
    private val _listUiState = MutableStateFlow<ConcertListUiState>(ConcertListUiState.Loading)
    val listUiState: StateFlow<ConcertListUiState> = _listUiState.asStateFlow()

    // --- Деталі концерту ---
    private val _detailUiState = MutableStateFlow<ConcertDetailUiState>(ConcertDetailUiState.Loading)
    val detailUiState: StateFlow<ConcertDetailUiState> = _detailUiState.asStateFlow()

    // --- Операції (блокування кнопок під час запиту) ---
    private val _isOperationInProgress = MutableStateFlow(false)
    val isOperationInProgress: StateFlow<Boolean> = _isOperationInProgress.asStateFlow()

    // ЛР №11 Завдання 4: Pull-to-refresh стан
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ЛР №11 Завдання 5: Набір ID концертів, що додані до "Обраного" (in-memory)
    private val _favoriteIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteIds: StateFlow<Set<Int>> = _favoriteIds.asStateFlow()

    // --- Одноразові події ---
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    // Сумісність із попередніми ЛР
    val concerts: StateFlow<List<Concert>> = _listUiState
        .map { state -> if (state is ConcertListUiState.Success) state.concerts else emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading: StateFlow<Boolean> = _listUiState
        .map { it is ConcertListUiState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        loadConcerts()
    }

    // ЛР №1 Завдання 4 (Рефакторинг): Extract Method — спільна обробка NetworkResult
    // для loadConcerts() і refreshConcerts(). Когнітивна складність знижена.
    // ДО: логіка when{} дублювалась у двох місцях (~12 рядків × 2).
    // ПІСЛЯ: один метод, кожен виклик — 1 рядок.
    private suspend fun handleConcertListResult(result: NetworkResult<List<Concert>>) {
        when (result) {
            is NetworkResult.Success -> {
                val list = result.data
                _listUiState.value = if (list.isEmpty()) ConcertListUiState.Empty
                else ConcertListUiState.Success(list, isOffline = false)
            }
            is NetworkResult.Error -> {
                // Fallback на кеш при помилці мережі (ЛР №9 Завдання 5)
                val cached = repository.cachedConcerts?.first()
                _listUiState.value = if (!cached.isNullOrEmpty())
                    ConcertListUiState.Success(cached, isOffline = true)
                else
                    ConcertListUiState.Error(result.message)
            }
            is NetworkResult.Loading -> Unit
        }
    }

    // ЛР №9 Завдання 3 + 5: Завантаження з мережі з fallback на кеш
    // ЛР №1 Рефакторинг: делегує у handleConcertListResult() (CC знижено)
    fun loadConcerts() {
        viewModelScope.launch {
            _listUiState.value = ConcertListUiState.Loading
            handleConcertListResult(repository.getAllConcerts())
        }
    }

    // ЛР №11 Завдання 4: Pull-to-refresh — оновлення через ViewModel → Repository → API
    // ЛР №1 Рефакторинг: делегує у handleConcertListResult() для списку (CC знижено)
    fun refreshConcerts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(800) // Візуальна затримка для наочності індикатора
            val result = repository.getAllConcerts()
            handleConcertListResult(result)
            // Додаткова поведінка refresh: Snackbar зі статусом
            when (result) {
                is NetworkResult.Success ->
                    _uiEvents.emit(UiEvent.ShowSnackbar("Список оновлено (${result.data.size} концертів)"))
                is NetworkResult.Error ->
                    _uiEvents.emit(UiEvent.ShowSnackbar("Не вдалося оновити: ${result.message}"))
                is NetworkResult.Loading -> Unit
            }
            _isRefreshing.value = false
        }
    }

    // ЛР №9 Завдання 3: Завантаження деталей з мережі за ID
    fun loadConcertDetail(id: Int) {
        viewModelScope.launch {
            _detailUiState.value = ConcertDetailUiState.Loading
            when (val result = repository.getConcertById(id)) {
                is NetworkResult.Success -> _detailUiState.value = ConcertDetailUiState.Success(result.data)
                is NetworkResult.Error -> _detailUiState.value = ConcertDetailUiState.Error(result.message)
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ЛР №9 Завдання 4: Додавання концерту через API
    fun addConcert(artist: String, title: String, date: String, genre: String, venue: String?) {
        viewModelScope.launch {
            _isOperationInProgress.value = true
            val dto = CreateConcertDto(
                artist = artist.trim(),
                title = title.trim(),
                date = date.trim(),
                genre = genre.trim(),
                venue = venue?.trim()?.ifEmpty { null }
            )
            when (val result = repository.createConcert(dto)) {
                is NetworkResult.Success -> {
                    _uiEvents.emit(UiEvent.ShowSnackbar("Концерт \"${result.data.title}\" додано!"))
                    _uiEvents.emit(UiEvent.NavigateBack)
                    loadConcerts() // Оновлюємо список
                }
                is NetworkResult.Error -> {
                    _uiEvents.emit(UiEvent.ShowSnackbar("Помилка додавання: ${result.message}"))
                }
                is NetworkResult.Loading -> Unit
            }
            _isOperationInProgress.value = false
        }
    }

    // ЛР №9 Завдання 4: Видалення концерту через API
    fun deleteConcert(id: Int) {
        viewModelScope.launch {
            _isOperationInProgress.value = true
            when (val result = repository.deleteConcert(id)) {
                is NetworkResult.Success -> {
                    // Також видаляємо з обраного, якщо був там
                    _favoriteIds.value = _favoriteIds.value - id
                    _uiEvents.emit(UiEvent.ShowSnackbar("Концерт видалено"))
                    loadConcerts()
                }
                is NetworkResult.Error -> {
                    _uiEvents.emit(UiEvent.ShowSnackbar("Помилка видалення: ${result.message}"))
                }
                is NetworkResult.Loading -> Unit
            }
            _isOperationInProgress.value = false
        }
    }

    // ЛР №11 Завдання 5: Додавання/видалення з «Обраного» через ViewModel
    fun toggleFavorite(id: Int) {
        viewModelScope.launch {
            val current = _favoriteIds.value
            if (id in current) {
                _favoriteIds.value = current - id
                _uiEvents.emit(UiEvent.ShowSnackbar("Видалено з обраного"))
            } else {
                _favoriteIds.value = current + id
                _uiEvents.emit(UiEvent.ShowSnackbar("Додано до обраного ⭐"))
            }
        }
    }

    // ЛР №12 Завдання 3: Збереження шляху до фото через MVVM-архітектуру
    fun saveImagePath(concertId: Int, imagePath: String?) {
        viewModelScope.launch {
            repository.saveImagePath(concertId, imagePath)
            // Реактивно оновлюємо список
            loadConcerts()
        }
    }
}