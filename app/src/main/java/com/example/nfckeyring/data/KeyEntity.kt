package com.example.nfckeyring.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "keys")
data class KeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val value: String
)
