package com.luvia.nfckeychain.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.content.Intent
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HceService : HostApduService() {
    
    companion object {
        // APDU commands
        private val SELECT_OK_RESPONSE = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val UNKNOWN_CMD_RESPONSE = byteArrayOf(0x00.toByte(), 0x00.toByte())
        private val SELECT_APDU_HEADER = byteArrayOf(0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte())
        
        // Application IDs (AIDs)
        private val KEYCHAIN_AID = "F0394148148100"
        private val ISO_AID = "F0010203040506"
        
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
            println("DEBUG: HCE emulation started for tag: $tagId")
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
        
        // Check if this is a SELECT command
        if (commandApdu.size >= 4 && 
            commandApdu[0] == SELECT_APDU_HEADER[0] &&
            commandApdu[1] == SELECT_APDU_HEADER[1] &&
            commandApdu[2] == SELECT_APDU_HEADER[2] &&
            commandApdu[3] == SELECT_APDU_HEADER[3]) {
            
            // Extract AID from SELECT command
            if (commandApdu.size >= 5) {
                val aidLength = commandApdu[4].toInt() and 0xFF
                if (commandApdu.size >= 5 + aidLength) {
                    val aid = commandApdu.sliceArray(5 until 5 + aidLength)
                    val aidHex = bytesToHex(aid)
                    println("DEBUG: SELECT command for AID: $aidHex")
                    
                    // Check if the AID matches our supported AIDs
                    if (aidHex == KEYCHAIN_AID || aidHex == ISO_AID) {
                        println("DEBUG: AID matches, sending OK response")
                        return SELECT_OK_RESPONSE
                    }
                }
            }
        }
        
        // Handle data read commands
        if (_isEmulating.value && _currentEmulatedData.value != null) {
            val emulatedData = _currentEmulatedData.value!!
            println("DEBUG: Returning emulated data: ${bytesToHex(emulatedData)}")
            
            // Return the emulated data with success status
            return emulatedData + SELECT_OK_RESPONSE
        }
        
        println("DEBUG: Unknown command or no emulation active")
        return UNKNOWN_CMD_RESPONSE
    }
    
    override fun onDeactivated(reason: Int) {
        println("DEBUG: HCE deactivated with reason: $reason")
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
}
