package com.example.myapplication7.ui.viewmodels

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// ЛР №12 Завдання 4: Стани UI геолокації
sealed class LocationUiState {
    object Idle : LocationUiState()
    object Loading : LocationUiState()
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val timestamp: Long,
        val distanceToTarget: Float   // Відстань до НСК Олімпійський
    ) : LocationUiState()
    data class Error(val message: String) : LocationUiState()
}

// ЛР №12: ViewModel для геолокації та фото
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        // ЛР №12 Завдання 4: Фіксована точка — НСК Олімпійський (місце концертів)
        const val TARGET_LAT = 50.4337
        const val TARGET_LON = 30.5214
        const val TARGET_NAME = "НСК Олімпійський, Київ"
    }

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    // --- Стан геолокації ---
    private val _locationState = MutableStateFlow<LocationUiState>(LocationUiState.Idle)
    val locationState: StateFlow<LocationUiState> = _locationState.asStateFlow()

    // ЛР №12 Завдання 3: Шлях до вибраного фото (зберігається в сесії)
    private val _selectedPhotoPath = MutableStateFlow<String?>(null)
    val selectedPhotoPath: StateFlow<String?> = _selectedPhotoPath.asStateFlow()

    // ЛР №12 Завдання 4: Отримання поточної геолокації через FusedLocationProviderClient
    fun fetchLocation() {
        viewModelScope.launch {
            _locationState.value = LocationUiState.Loading
            try {
                val location = fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .await()

                if (location != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        location.latitude, location.longitude,
                        TARGET_LAT, TARGET_LON,
                        results
                    )
                    _locationState.value = LocationUiState.Success(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        timestamp = location.time,
                        distanceToTarget = results[0]
                    )
                } else {
                    _locationState.value = LocationUiState.Error(
                        "Не вдалося отримати локацію. Переконайтеся, що GPS увімкнено."
                    )
                }
            } catch (e: SecurityException) {
                _locationState.value = LocationUiState.Error("Відсутній дозвіл на геолокацію")
            } catch (e: Exception) {
                _locationState.value = LocationUiState.Error("Помилка: ${e.localizedMessage}")
            }
        }
    }

    // ЛР №12 Завдання 3: Копіювання вибраного зображення до filesDir
    fun savePhotoFromUri(context: Context, uri: android.net.Uri): String? {
        return try {
            val dir = File(context.filesDir, "concert_photos").apply { mkdirs() }
            val fileName = "photo_${UUID.randomUUID()}.jpg"
            val destFile = File(dir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath.also { path ->
                _selectedPhotoPath.value = path
            }
        } catch (e: Exception) {
            null
        }
    }

    // Очищення поточного фото
    fun clearPhoto() {
        _selectedPhotoPath.value = null
    }
}
