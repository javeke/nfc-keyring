package com.example.nfckeyring.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uid: String,
    val type: String,
    val payload: String,
    val label: String,
    val createdAt: Long
)
