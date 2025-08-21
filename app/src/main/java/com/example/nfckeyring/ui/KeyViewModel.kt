package com.example.nfckeyring.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfckeyring.data.AppDatabase
import com.example.nfckeyring.data.KeyEntity
import com.example.nfckeyring.data.KeyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class KeyViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var repository: KeyRepository
    lateinit var allKeys: Flow<List<KeyEntity>>

    fun initialize() {
        if (this::repository.isInitialized) return
        val dao = AppDatabase.getDatabase(getApplication()).keyDao()
        repository = KeyRepository(dao)
        allKeys = repository.allKeys
    }

    fun insert(key: KeyEntity) = viewModelScope.launch {
        if (this::repository.isInitialized) {
            repository.insert(key)
        }
    }

    fun delete(key: KeyEntity) = viewModelScope.launch {
        if (this::repository.isInitialized) {
            repository.delete(key)
        }
    }
}
