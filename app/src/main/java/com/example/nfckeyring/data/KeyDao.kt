package com.example.nfckeyring.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyDao {
    @Query("SELECT * FROM keys")
    fun getAllKeys(): Flow<List<KeyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: KeyEntity)

    @Delete
    suspend fun delete(key: KeyEntity)
}
