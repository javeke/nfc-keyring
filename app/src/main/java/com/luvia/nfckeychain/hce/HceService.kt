package com.luvia.nfckeychain.hce

import android.nfc.NdefMessage
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.content.Intent
import com.luvia.nfckeychain.data.prefs.Preferences
import com.luvia.nfckeychain.nfc.utils.NfcUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HceService : HostApduService() {
    
    companion object {
        // APDU commands
        private val SELECT_OK_RESPONSE = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val UNKNOWN_CMD_RESPONSE = byteArrayOf(0x6F.toByte(), 0x00.toByte()) // SW_UNKNOWN
        private val SELECT_APDU_HEADER = byteArrayOf(0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte())
        
        // NDEF-specific constants
        private val NDEF_AID = byteArrayOf(0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(), 0x01.toByte(), 0x01.toByte())
        private val NDEF_AID_ALT = byteArrayOf(0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(), 0x01.toByte(), 0x00.toByte())
        private val SELECT_NDEF_CMD = byteArrayOf(0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte())
        private val READ_BINARY_CMD = byteArrayOf(0x00.toByte(), 0xB0.toByte())
        private val GET_VERSION_CMD = byteArrayOf(0x60.toByte(), 0x00.toByte())
        
        // Application IDs (AIDs)
        private val KEYCHAIN_AID = "F0394148148100"
        private val ISO_AID = "F0010203040506"
        // NFC Forum Type 4 Tag file IDs
        private val CC_FILE_ID = byteArrayOf(0xE1.toByte(), 0x03.toByte())
        private val NDEF_FILE_ID = byteArrayOf(0xE1.toByte(), 0x04.toByte())

        // File selection identifiers
        private const val FILE_NONE = 0
        private const val FILE_CC = 1
        private const val FILE_NDEF = 2

        // Auto-stop after first successful read flag
        private val _autoStopOnRead = MutableStateFlow(false)
        val autoStopOnRead: StateFlow<Boolean> = _autoStopOnRead.asStateFlow()
        fun setAutoStopOnRead(enabled: Boolean) {
            _autoStopOnRead.value = enabled
            println("DEBUG: autoStopOnRead set to $enabled")
        }
        
        // Emulation state
        private val _isEmulating = MutableStateFlow(false)
        val isEmulating: StateFlow<Boolean> = _isEmulating.asStateFlow()
        
        private val _currentEmulatedData = MutableStateFlow<ByteArray?>(null)
        val currentEmulatedData: StateFlow<ByteArray?> = _currentEmulatedData.asStateFlow()
        
        private val _emulatedTagId = MutableStateFlow<String?>(null)
        val emulatedTagId: StateFlow<String?> = _emulatedTagId.asStateFlow()
        
        // Static methods to control emulation from the app
        fun startEmulation(tagId: String, data: ByteArray) {
            _emulatedTagId.value = tagId
            _currentEmulatedData.value = data
            _isEmulating.value = true
            println("DEBUG: HCE emulation started for tag: $tagId bytes=${data.size}")
        }
        
        fun stopEmulation() {
            _emulatedTagId.value = null
            _currentEmulatedData.value = null
            _isEmulating.value = false
            println("DEBUG: HCE emulation stopped")
        }
        
        fun getEmulationStatus(): Boolean = _isEmulating.value
        
        private fun hexStringToByteArray(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }
    }

    // Selected file tracking and computed file contents
    private var selectedFile: Int = FILE_NONE
    private val ccFileBytes: ByteArray
        get() = buildCcFile(maxNdefFileSize = 1024)
    private val ndefFileBytes: ByteArray
        get() = buildNdefFile()
    
    override fun onCreate() {
        super.onCreate()
        println("DEBUG: HceService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("DEBUG: HceService onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }
    
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        println("DEBUG: HCE received APDU command: ${commandApdu?.let { bytesToHex(it) }}")
        
        if (commandApdu == null) {
            println("DEBUG: Command APDU is null")
            return UNKNOWN_CMD_RESPONSE
        }
        
        // Handle NDEF SELECT command
        if (commandApdu.size >= 5 && 
            commandApdu[0] == 0x00.toByte() && 
            commandApdu[1] == 0xA4.toByte() && 
            commandApdu[2] == 0x04.toByte() && 
            commandApdu[3] == 0x00.toByte() &&
            commandApdu[4] == 0x07.toByte()) {
            
            // Check if this is a SELECT NDEF command
            if (commandApdu.size >= 12) {
                val aid = commandApdu.sliceArray(5..11)
                if (aid.contentEquals(NDEF_AID) || aid.contentEquals(NDEF_AID_ALT)) {
                    println("DEBUG: SELECT NDEF command received for AID: ${bytesToHex(aid)}")
                    return SELECT_OK_RESPONSE
                }
            }
        }
        
        // Handle GET VERSION command
        if (commandApdu.size >= 2 && 
            commandApdu[0] == 0x60.toByte() && 
            commandApdu[1] == 0x00.toByte()) {
            println("DEBUG: GET VERSION command received")
            // Return NDEF version info
            val versionResponse = byteArrayOf(0x00.toByte(), 0x03.toByte(), 0x03.toByte(), 0x00.toByte())
            return versionResponse + SELECT_OK_RESPONSE
        }
        
        // Handle READ BINARY (Type 4 Tag with offsets and selected file)
        if (commandApdu.size >= 4 &&
            commandApdu[0] == 0x00.toByte() &&
            commandApdu[1] == 0xB0.toByte()) {
            val offset = ((commandApdu[2].toInt() and 0xFF) shl 8) or (commandApdu[3].toInt() and 0xFF)
            val le = if (commandApdu.size >= 5) (commandApdu[4].toInt() and 0xFF).let { if (it == 0) 256 else it } else 0xFF
            val fileBytes = when (selectedFile) {
                FILE_CC -> ccFileBytes
                FILE_NDEF -> ndefFileBytes
                else -> ByteArray(0)
            }
            if (fileBytes.isEmpty()) {
                println("DEBUG: READ BINARY requested but no file selected")
                return UNKNOWN_CMD_RESPONSE
            }
            if (offset >= fileBytes.size) {
                println("DEBUG: READ BINARY offset beyond file size")
                return SELECT_OK_RESPONSE
            }
            val end = kotlin.math.min(offset + le, fileBytes.size)
            val chunk = fileBytes.copyOfRange(offset, end)
            val response = chunk + SELECT_OK_RESPONSE
            // If auto-stop is enabled and we're reading NDEF file, stop emulation after serving data
//            if (selectedFile == FILE_NDEF && _autoStopOnRead.value) {
//                println("DEBUG: Auto-stop after read triggered")
//                stopEmulation()
//            }
            return response
        }
        
        // Handle SELECT APPLICATION command (alternative NDEF selection)
        if (commandApdu.size >= 5 && 
            commandApdu[0] == 0x00.toByte() && 
            commandApdu[1] == 0xA4.toByte() && 
            commandApdu[2] == 0x04.toByte() && 
            commandApdu[3] == 0x00.toByte()) {
            
            val aidLength = commandApdu[4].toInt() and 0xFF
            if (commandApdu.size >= 5 + aidLength) {
                val aid = commandApdu.sliceArray(5 until 5 + aidLength)
                val aidHex = bytesToHex(aid)
                println("DEBUG: SELECT APPLICATION command for AID: $aidHex")
                
                // Check if the AID matches NDEF or our supported AIDs
                if (aid.contentEquals(NDEF_AID) || aid.contentEquals(NDEF_AID_ALT) || aidHex == KEYCHAIN_AID || aidHex == ISO_AID) {
                    println("DEBUG: AID matches, sending OK response for AID: $aidHex")
                    selectedFile = FILE_NONE
                    return SELECT_OK_RESPONSE
                }
            }
        }
        
        // Handle SELECT FILE command (for CC and NDEF file selection)
        if (commandApdu.size >= 5 && 
            commandApdu[0] == 0x00.toByte() && 
            commandApdu[1] == 0xA4.toByte() && 
            commandApdu[2] == 0x00.toByte() && 
            commandApdu[3] == 0x0C.toByte()) {
            
            println("DEBUG: SELECT FILE command received")
            val lc = commandApdu.getOrNull(4)?.toInt() ?: 0
            if (lc == 0x02) {
                val fid = commandApdu.sliceArray(5 until 7)
                when {
                    fid.contentEquals(CC_FILE_ID) -> {
                        selectedFile = FILE_CC
                        println("DEBUG: CC file selected")
                        return SELECT_OK_RESPONSE
                    }
                    fid.contentEquals(NDEF_FILE_ID) -> {
                        selectedFile = FILE_NDEF
                        println("DEBUG: NDEF file selected")
                        return SELECT_OK_RESPONSE
                    }
                    else -> {
                        println("DEBUG: Unknown file selected: ${bytesToHex(fid)}")
                        return UNKNOWN_CMD_RESPONSE
                    }
                }
            }
            return UNKNOWN_CMD_RESPONSE
        }
        
        // Handle GET UID command (some readers request this)
        if (commandApdu.size >= 2 && 
            commandApdu[0] == 0xFF.toByte() && 
            commandApdu[1] == 0xCA.toByte()) {
            println("DEBUG: GET UID command received")
            // Return a dummy UID
            val uid = byteArrayOf(0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte())
            return uid + SELECT_OK_RESPONSE
        }
        
        // Unknown command
        
        println("DEBUG: Unknown command: ${bytesToHex(commandApdu)}")
        return UNKNOWN_CMD_RESPONSE
    }
    
    override fun onDeactivated(reason: Int) {
        println("DEBUG: HCE deactivated with reason: $reason")
        when (reason) {
            DEACTIVATION_LINK_LOSS -> println("DEBUG: Deactivation reason: LINK_LOSS")
            DEACTIVATION_DESELECTED -> println("DEBUG: Deactivation reason: DESELECTED") 
            else -> println("DEBUG: Deactivation reason: UNKNOWN ($reason)")
        }
        // Don't stop emulation on deactivation, keep it running until user stops it
    }
    
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789ABCDEF"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }
    
    // Build CC file content according to NFC Forum Type 4 Tag spec
    private fun buildCcFile(maxNdefFileSize: Int): ByteArray {
        val cclenHi = 0x00.toByte()
        val cclenLo = 0x0F.toByte() // 15 bytes
        val mappingVersion = 0x20.toByte() // Version 2.0
        val mLeHi = 0x00.toByte()
        val mLeLo = 0xFF.toByte()
        val mLcHi = 0x00.toByte()
        val mLcLo = 0xFF.toByte()
        val t = 0x04.toByte() // NDEF File Control TLV
        val l = 0x06.toByte()
        val fidHi = NDEF_FILE_ID[0]
        val fidLo = NDEF_FILE_ID[1]
        val maxHi = ((maxNdefFileSize ushr 8) and 0xFF).toByte()
        val maxLo = (maxNdefFileSize and 0xFF).toByte()
        val readAccess = 0x00.toByte()
        val writeAccess = 0x00.toByte()
        return byteArrayOf(
            cclenHi, cclenLo, mappingVersion,
            mLeHi, mLeLo, mLcHi, mLcLo,
            t, l, fidHi, fidLo, maxHi, maxLo, readAccess, writeAccess
        )
    }

    // Build NDEF file: 2-byte NLEN + NDEF message body
    private fun buildNdefFile(): ByteArray {
        val body = buildNdefBody()
        val nlenHi = ((body.size ushr 8) and 0xFF).toByte()
        val nlenLo = (body.size and 0xFF).toByte()
        return byteArrayOf(nlenHi, nlenLo) + body
    }

    // Use provided data as NDEF if valid; else build a minimal Text NDEF
    private fun buildNdefBody(): ByteArray {
        var bytes: ByteArray? = currentEmulatedData.value
        if (bytes == null) {
            // Attempt to backfill from favorite (tile/widget path)
            Preferences.getFavoriteDataHex(this)?.let { hex ->
                val parsed = runCatching { NfcUtils.hexToBytes(hex) }.getOrNull()
                if (parsed != null) {
                    _currentEmulatedData.value = parsed
                    bytes = parsed
                    println("DEBUG: Backfilled emulation bytes from favorite, len=${parsed.size}")
                }
            }
        }
        val data = bytes
        if (data != null) {
            // Normalize legacy TLV-wrapped payload [0x00, len, message]
            val normalized = if (data.size >= 3 && data[0] == 0x00.toByte()) {
                val len = (data[1].toInt() and 0xFF)
                if (len == data.size - 2) data.copyOfRange(2, data.size) else data
            } else data
            return try {
                val msg = NdefMessage(normalized)
                println("DEBUG: Using provided NDEF payload, length=${normalized.size}")
                msg.toByteArray()
            } catch (e: Exception) {
                println("DEBUG: Provided data not valid NDEF (${e.message}), falling back")
                buildFallbackTextNdef()
            }
        }
        // No data at all → fallback text NDEF
        return buildFallbackTextNdef()
    }

    private fun buildFallbackTextNdef(): ByteArray {
        val languageCode = "en".toByteArray(Charsets.US_ASCII)
        val textBytes = "NFC Keychain".toByteArray(Charsets.UTF_8)
        val status = (languageCode.size and 0x3F).toByte()
        val payload = byteArrayOf(status) + languageCode + textBytes
        val header = 0xD1.toByte() // MB=1, ME=1, SR=1, TNF=1
        val type = 0x54.toByte()   // 'T'
        return byteArrayOf(
            header,
            0x01, // type length
            payload.size.toByte(),
            type
        ) + payload
    }

    // Selected file tracking moved to companion and property above
}


