package com.example.nfckeyring.data

import kotlinx.coroutines.flow.Flow

class KeyRepository(private val keyDao: KeyDao) {
    val allKeys: Flow<List<KeyEntity>> = keyDao.getAllKeys()

    suspend fun insert(key: KeyEntity) = keyDao.insert(key)

    suspend fun delete(key: KeyEntity) = keyDao.delete(key)
}
