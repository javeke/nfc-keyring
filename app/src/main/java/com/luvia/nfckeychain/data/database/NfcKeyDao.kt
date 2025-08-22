package com.luvia.nfckeychain.data.database

import androidx.room.*
import com.luvia.nfckeychain.data.model.NfcKey
import kotlinx.coroutines.flow.Flow

@Dao
interface NfcKeyDao {
    @Query("SELECT * FROM nfc_keys ORDER BY createdAt DESC")
    fun getAllKeys(): Flow<List<NfcKey>>
    
    @Query("SELECT * FROM nfc_keys WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveKeys(): Flow<List<NfcKey>>
    
    @Query("SELECT * FROM nfc_keys WHERE id = :id")
    suspend fun getKeyById(id: Long): NfcKey?
    
    @Query("SELECT * FROM nfc_keys WHERE tagId = :tagId")
    suspend fun getKeyByTagId(tagId: String): NfcKey?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: NfcKey): Long
    
    @Update
    suspend fun updateKey(key: NfcKey)
    
    @Delete
    suspend fun deleteKey(key: NfcKey)
    
    @Query("DELETE FROM nfc_keys WHERE id = :id")
    suspend fun deleteKeyById(id: Long)
    
    @Query("UPDATE nfc_keys SET isActive = :isActive WHERE id = :id")
    suspend fun updateKeyStatus(id: Long, isActive: Boolean)
}
