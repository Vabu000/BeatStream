package com.example.myapplication7.data

// ЛР №9 Завдання 5: Запечатаний клас для станів мережевої відповіді
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
}

// Хелпер для безпечного виклику API
suspend fun <T> safeApiCall(apiCall: suspend () -> retrofit2.Response<T>): NetworkResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                NetworkResult.Success(body)
            } else {
                NetworkResult.Error("Порожня відповідь сервера", response.code())
            }
        } else {
            NetworkResult.Error(
                message = response.errorBody()?.string() ?: "Помилка сервера",
                code = response.code()
            )
        }
    } catch (e: java.net.UnknownHostException) {
        NetworkResult.Error("Немає підключення до мережі")
    } catch (e: java.net.SocketTimeoutException) {
        NetworkResult.Error("Час очікування вичерпано. Перевірте з'єднання")
    } catch (e: Exception) {
        NetworkResult.Error(e.localizedMessage ?: "Невідома помилка")
    }
}