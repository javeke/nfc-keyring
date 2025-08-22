package com.luvia.nfckeychain.nfc.utils

import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.IsoDep
import com.luvia.nfckeychain.data.model.NfcTagInfo

object NfcUtils {
    
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789ABCDEF"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }
    
    fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
    
    fun getTagInfo(tag: Tag): NfcTagInfo {
        val tagId = bytesToHex(tag.id)
        val technologies = tag.techList.toList()
        val tagType = when {
            technologies.contains("android.nfc.tech.Ndef") -> "NDEF"
            technologies.contains("android.nfc.tech.MifareClassic") -> "MifareClassic"
            technologies.contains("android.nfc.tech.MifareUltralight") -> "MifareUltralight"
            technologies.contains("android.nfc.tech.IsoDep") -> "IsoDep"
            else -> "Unknown"
        }
        
        val isWritable = isTagWritable(tag)
        
        return NfcTagInfo(
            tagId = tagId,
            tagType = tagType,
            technologies = technologies,
            maxTransceiveLength = 253, // Default value for most NFC tags
            isWritable = isWritable
        )
    }
    
    private fun isTagWritable(tag: Tag): Boolean {
        return try {
            val ndef = Ndef.get(tag)
            ndef?.isWritable ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    fun readNdefMessage(tag: Tag): String? {
        return try {
            val ndef = Ndef.get(tag)
            ndef?.let {
                it.connect()
                val ndefMessage = it.ndefMessage
                it.close()
                ndefMessage?.toString()
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun getTagTechnology(tag: Tag, techClass: Class<*>): Any? {
        return try {
            when (techClass) {
                Ndef::class.java -> Ndef.get(tag)
                NdefFormatable::class.java -> NdefFormatable.get(tag)
                MifareClassic::class.java -> MifareClassic.get(tag)
                MifareUltralight::class.java -> MifareUltralight.get(tag)
                IsoDep::class.java -> IsoDep.get(tag)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
