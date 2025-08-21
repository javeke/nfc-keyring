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

    private val repository: KeyRepository
    val allKeys: Flow<List<KeyEntity>>

    init {
        val dao = AppDatabase.getDatabase(application).keyDao()
        repository = KeyRepository(dao)
        allKeys = repository.allKeys
    }

    fun insert(key: KeyEntity) = viewModelScope.launch {
        repository.insert(key)
    }

    fun delete(key: KeyEntity) = viewModelScope.launch {
        repository.delete(key)
    }
}
