package com.example.nfckeyring.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfckeyring.data.AppDatabase
import com.example.nfckeyring.data.TagEntity
import com.example.nfckeyring.data.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TagViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var repository: TagRepository
    lateinit var allTags: Flow<List<TagEntity>>

    fun initialize() {
        if (this::repository.isInitialized) return
        val dao = AppDatabase.getDatabase(getApplication()).tagDao()
        repository = TagRepository(dao)
        allTags = repository.allTags
    }

    fun insert(tag: TagEntity) = viewModelScope.launch {
        if (this::repository.isInitialized) {
            repository.insert(tag)
        }
    }

    fun update(tag: TagEntity) = viewModelScope.launch {
        if (this::repository.isInitialized) {
            repository.update(tag)
        }
    }

    fun delete(tag: TagEntity) = viewModelScope.launch {
        if (this::repository.isInitialized) {
            repository.delete(tag)
        }
    }
}
