package com.luvia.nfckeychain.data.repository

import com.luvia.nfckeychain.data.database.NfcKeyDao
import com.luvia.nfckeychain.data.model.NfcKey
import kotlinx.coroutines.flow.Flow

class NfcKeyRepository(
    private val nfcKeyDao: NfcKeyDao
) {
    fun getAllKeys(): Flow<List<NfcKey>> = nfcKeyDao.getAllKeys()
    
    fun getActiveKeys(): Flow<List<NfcKey>> = nfcKeyDao.getActiveKeys()
    
    suspend fun getKeyById(id: Long): NfcKey? = nfcKeyDao.getKeyById(id)
    
    suspend fun getKeyByTagId(tagId: String): NfcKey? = nfcKeyDao.getKeyByTagId(tagId)
    
    suspend fun insertKey(key: NfcKey): Long = nfcKeyDao.insertKey(key)
    
    suspend fun updateKey(key: NfcKey) = nfcKeyDao.updateKey(key)
    
    suspend fun deleteKey(key: NfcKey) = nfcKeyDao.deleteKey(key)
    
    suspend fun deleteKeyById(id: Long) = nfcKeyDao.deleteKeyById(id)
    
    suspend fun updateKeyStatus(id: Long, isActive: Boolean) = nfcKeyDao.updateKeyStatus(id, isActive)
}
