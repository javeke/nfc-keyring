package com.luvia.nfckeychain.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luvia.nfckeychain.data.database.AppDatabase
import com.luvia.nfckeychain.data.model.NfcKey
import com.luvia.nfckeychain.data.model.NfcTagInfo
import com.luvia.nfckeychain.data.repository.NfcKeyRepository
import com.luvia.nfckeychain.security.BiometricManager
import com.luvia.nfckeychain.security.BiometricResult
import com.luvia.nfckeychain.emulation.EmulationManager
import com.luvia.nfckeychain.emulation.EmulationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KeyViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = NfcKeyRepository(database.nfcKeyDao())
    private val biometricManager = BiometricManager(application)
    private val emulationManager = EmulationManager(application)
    
    private val _keys = MutableStateFlow<List<NfcKey>>(emptyList())
    val keys: StateFlow<List<NfcKey>> = _keys.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _lastScannedTag = MutableStateFlow<NfcTagInfo?>(null)
    val lastScannedTag: StateFlow<NfcTagInfo?> = _lastScannedTag.asStateFlow()
    
    // Authentication state
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()
    
    private val _biometricAvailable = MutableStateFlow(false)
    val biometricAvailable: StateFlow<Boolean> = _biometricAvailable.asStateFlow()
    
    // Emulation state
    val isEmulating: StateFlow<Boolean> = emulationManager.isEmulating
    val emulatedTagId: StateFlow<String?> = emulationManager.emulatedTagId
    
    private val _emulationMessage = MutableStateFlow<String?>(null)
    val emulationMessage: StateFlow<String?> = _emulationMessage.asStateFlow()
    
    init {
        checkBiometricAvailability()
        if (biometricManager.isBiometricAvailable()) {
            _biometricAvailable.value = true
        }
    }
    
    private fun checkBiometricAvailability() {
        _biometricAvailable.value = biometricManager.isBiometricAvailable()
    }
    
    fun authenticate(activity: androidx.fragment.app.FragmentActivity) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            try {
                val result = biometricManager.authenticate(activity)
                when (result) {
                    is BiometricResult.Success -> {
                        println("DEBUG: Authentication successful, setting isAuthenticated = true")
                        _isAuthenticated.value = true
                        loadKeys()
                    }
                    is BiometricResult.Failed -> {
                        // Authentication failed, stay locked
                        println("DEBUG: Authentication failed, setting isAuthenticated = false")
                        _isAuthenticated.value = false
                    }
                    is BiometricResult.Error -> {
                        // Handle specific errors
                        println("Biometric error: ${result.errorMessage}")
                        println("DEBUG: Authentication error, setting isAuthenticated = false")
                        _isAuthenticated.value = false
                    }
                }
            } catch (e: Exception) {
                println("Authentication error: ${e.message}")
                _isAuthenticated.value = false
            } finally {
                _isAuthenticating.value = false
            }
        }
    }
    
    fun loadKeys() {
        if (!_isAuthenticated.value) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Start collecting the flow in a separate coroutine
                launch {
                    repository.getActiveKeys().collect { keyList ->
                        _keys.value = keyList
                    }
                }
            } catch (e: Exception) {
                println("Error loading keys: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addKey(name: String, description: String?, tagInfo: NfcTagInfo, data: String) {
        if (!_isAuthenticated.value) return
        
        viewModelScope.launch {
            try {
                val newKey = NfcKey(
                    name = name,
                    description = description,
                    tagId = tagInfo.tagId,
                    tagType = tagInfo.tagType,
                    data = data
                )
                repository.insertKey(newKey)
                // The Flow will automatically update the keys list
            } catch (e: Exception) {
                println("Error adding key: ${e.message}")
            }
        }
    }
    
    fun updateKey(key: NfcKey) {
        if (!_isAuthenticated.value) return
        
        viewModelScope.launch {
            try {
                repository.updateKey(key.copy(updatedAt = System.currentTimeMillis()))
                // The Flow will automatically update the keys list
            } catch (e: Exception) {
                println("Error updating key: ${e.message}")
            }
        }
    }
    
    fun deleteKey(key: NfcKey) {
        if (!_isAuthenticated.value) return
        
        viewModelScope.launch {
            try {
                repository.deleteKey(key)
                // The Flow will automatically update the keys list
            } catch (e: Exception) {
                println("Error deleting key: ${e.message}")
            }
        }
    }
    
    fun toggleKeyStatus(key: NfcKey) {
        if (!_isAuthenticated.value) return
        
        viewModelScope.launch {
            try {
                repository.updateKeyStatus(key.id, !key.isActive)
                // The Flow will automatically update the keys list
            } catch (e: Exception) {
                println("Error toggling key status: ${e.message}")
            }
        }
    }
    
    fun setLastScannedTag(tagInfo: NfcTagInfo) {
        println("DEBUG: setLastScannedTag called, isAuthenticated: ${_isAuthenticated.value}")
        if (!_isAuthenticated.value) return
        _lastScannedTag.value = tagInfo
    }
    
    fun clearLastScannedTag() {
        _lastScannedTag.value = null
    }
    
    fun getKeyByTagId(tagId: String): NfcKey? {
        return _keys.value.find { it.tagId == tagId }
    }
    
    fun lockApp() {
        // Stop any active emulation when locking
        stopEmulation()
        _isAuthenticated.value = false
        _keys.value = emptyList()
        _lastScannedTag.value = null
    }
    
    // Emulation methods
    fun startEmulation(nfcKey: NfcKey) {
        if (!_isAuthenticated.value) return
        
        viewModelScope.launch {
            try {
                val result = emulationManager.startEmulation(nfcKey)
                when (result) {
                    is EmulationResult.Success -> {
                        _emulationMessage.value = result.message
                        println("DEBUG: Emulation started: ${result.message}")
                    }
                    is EmulationResult.Error -> {
                        _emulationMessage.value = result.message
                        println("DEBUG: Emulation error: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _emulationMessage.value = "Failed to start emulation: ${e.message}"
                println("DEBUG: Emulation exception: ${e.message}")
            }
        }
    }
    
    fun stopEmulation() {
        viewModelScope.launch {
            try {
                val result = emulationManager.stopEmulation()
                when (result) {
                    is EmulationResult.Success -> {
                        _emulationMessage.value = result.message
                        println("DEBUG: Emulation stopped: ${result.message}")
                    }
                    is EmulationResult.Error -> {
                        _emulationMessage.value = result.message
                        println("DEBUG: Stop emulation error: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                _emulationMessage.value = "Failed to stop emulation: ${e.message}"
                println("DEBUG: Stop emulation exception: ${e.message}")
            }
        }
    }
    
    fun isKeyBeingEmulated(nfcKey: NfcKey): Boolean {
        return emulationManager.isEmulating(nfcKey)
    }
    
    fun clearEmulationMessage() {
        _emulationMessage.value = null
    }
}
