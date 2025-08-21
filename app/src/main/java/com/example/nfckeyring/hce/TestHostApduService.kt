package com.example.nfckeyring.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.example.nfckeyring.data.AppDatabase
import com.example.nfckeyring.util.SecurePrefs
import kotlinx.coroutines.runBlocking

class TestHostApduService : HostApduService() {
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val selectApdu = buildSelectApdu(TEST_AID)
        if (commandApdu != null && commandApdu.contentEquals(selectApdu)) {
            val prefs = SecurePrefs.getPrefs(this)
            val selectedId = prefs.getInt("selected_tag_id", -1)
            if (selectedId != -1) {
                val db = AppDatabase.getDatabase(this)
                val tag = runBlocking { db.tagDao().getById(selectedId) }
                tag?.let {
                    val response = it.payload.hexToBytes()
                    return response + STATUS_SUCCESS
                }
            }
            return UNKNOWN_CMD_SW
        }
        return UNKNOWN_CMD_SW
    }

    override fun onDeactivated(reason: Int) {
        // No-op for demo
    }

    companion object {
        private const val TEST_AID = "F00102030405"
        private val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00)
        private val UNKNOWN_CMD_SW = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        private val SELECT_APDU_HEADER = byteArrayOf(0x00.toByte(), 0xA4.toByte(), 0x04, 0x00)

        private fun buildSelectApdu(aid: String): ByteArray {
            val aidBytes = aid.hexToBytes()
            val length = aidBytes.size.toByte()
            return SELECT_APDU_HEADER + byteArrayOf(length) + aidBytes
        }

        private fun String.hexToBytes(): ByteArray {
            val clean = replace(" ", "")
            check(clean.length % 2 == 0) { "Hex string must have even length" }
            return clean.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }
    }
}
