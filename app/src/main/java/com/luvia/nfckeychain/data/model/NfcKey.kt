package com.luvia.nfckeychain.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nfc_keys")
data class NfcKey(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val tagId: String, // NFC tag UID
    val tagType: String, // Type of NFC tag (NDEF, Mifare, etc.)
    val data: String, // Encrypted data stored on the tag
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
