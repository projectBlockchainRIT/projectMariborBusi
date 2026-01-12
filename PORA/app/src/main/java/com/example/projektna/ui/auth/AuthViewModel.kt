package com.example.projektna.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.projektna.data.PreferencesManager
import com.example.projektna.data.api.model.LoginResponse
import com.example.projektna.data.repository.AuthRepository
import com.example.projektna.util.Resource
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val preferencesManager = PreferencesManager(application)

    private val _loginResult = MutableLiveData<Resource<LoginResponse>>()
    val loginResult: LiveData<Resource<LoginResponse>> = _loginResult

    private val _registerResult = MutableLiveData<Resource<Unit>>()
    val registerResult: LiveData<Resource<Unit>> = _registerResult

    /**
     * Prijava uporabnika.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = Resource.Loading()
            val result = authRepository.login(email, password)

            // Ob uspešni prijavi shrani token in user ID
            if (result is Resource.Success && result.data != null) {
                preferencesManager.saveLoginData(
                    token = result.data.data.token,
                    visitorId = result.data.data.id,
                    email = email
                )
            }

            _loginResult.value = result
        }
    }

    /**
     * Registracija novega uporabnika.
     */
    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _registerResult.value = Resource.Loading()
            _registerResult.value = authRepository.register(username, email, password)
        }
    }

    /**
     * Odjava uporabnika.
     */
    fun logout() {
        preferencesManager.clearAuthData()
    }

    /**
     * Preveri, ali je uporabnik prijavljen.
     */
    fun isLoggedIn(): Boolean {
        return preferencesManager.isLoggedIn
    }

    /**
     * Vrne e-naslov prijavljenega uporabnika.
     */
    fun getUserEmail(): String? {
        return preferencesManager.userEmail
    }

    /**
     * Vrne uporabniško ime prijavljenega uporabnika.
     */
    fun getUsername(): String? {
        return preferencesManager.userUsername
    }

    /**
     * Resetira stanje prijave za ponovno uporabo.
     */
    fun resetLoginState() {
        _loginResult.value = null
    }

    /**
     * Resetira stanje registracije za ponovno uporabo.
     */
    fun resetRegisterState() {
        _registerResult.value = null
    }
}
