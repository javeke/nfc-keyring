package com.luvia.nfckeychain.emulation

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.content.ComponentName
import com.luvia.nfckeychain.data.model.NfcKey
import com.luvia.nfckeychain.hce.HceService
import com.luvia.nfckeychain.nfc.utils.NfcUtils
import kotlinx.coroutines.flow.StateFlow

class EmulationManager(private val activity: android.app.Activity) {
    
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val cardEmulation: CardEmulation? = CardEmulation.getInstance(nfcAdapter)
    private val hceServiceComponent = ComponentName(activity, HceService::class.java)
    
    // Expose emulation state from HceService
    val isEmulating: StateFlow<Boolean> = HceService.isEmulating
    val currentEmulatedData: StateFlow<ByteArray?> = HceService.currentEmulatedData
    val emulatedTagId: StateFlow<String?> = HceService.emulatedTagId
    
    /**
     * Check if NFC and HCE are available on this device
     */
    fun isHceAvailable(): Boolean {
        return nfcAdapter?.isEnabled == true && cardEmulation != null
    }
    
    /**
     * Check if NFC is enabled
     */
    fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }
    
    /**
     * Start emulating an NFC key
     */
    fun startEmulation(nfcKey: NfcKey): EmulationResult {
        if (!isHceAvailable()) {
            return EmulationResult.Error("NFC or HCE not available")
        }
        
        try {
            // Convert the stored data to byte array
            val emulatedData = if (nfcKey.data.isNotBlank()) {
                // If data is hex string, convert to bytes
                if (nfcKey.data.matches(Regex("^[0-9A-Fa-f]+$")) && nfcKey.data.length % 2 == 0) {
                    NfcUtils.hexToBytes(nfcKey.data)
                } else {
                    // If data is text, convert to bytes
                    nfcKey.data.toByteArray(Charsets.UTF_8)
                }
            } else {
                // Use tag ID as fallback data
                NfcUtils.hexToBytes(nfcKey.tagId)
            }
            
            // Start HCE emulation
            HceService.startEmulation(nfcKey.tagId, emulatedData)
            
            // Try to set this service as the preferred service (if supported)
            try {
                cardEmulation?.setPreferredService(activity, hceServiceComponent)
            } catch (e: Exception) {
                println("DEBUG: Could not set preferred service: ${e.message}")
                // This is not critical, continue anyway
            }
            
            return EmulationResult.Success("Emulating ${nfcKey.name}")
            
        } catch (e: Exception) {
            return EmulationResult.Error("Failed to start emulation: ${e.message}")
        }
    }
    
    /**
     * Stop emulating NFC tags
     */
    fun stopEmulation(): EmulationResult {
        try {
            HceService.stopEmulation()
            
            // Unset preferred service
            try {
                cardEmulation?.unsetPreferredService(activity)
            } catch (e: Exception) {
                println("DEBUG: Could not unset preferred service: ${e.message}")
                // This is not critical, continue anyway
            }
            
            return EmulationResult.Success("Emulation stopped")
            
        } catch (e: Exception) {
            return EmulationResult.Error("Failed to stop emulation: ${e.message}")
        }
    }
    
    /**
     * Get the currently emulated key ID
     */
    fun getCurrentEmulatedKeyId(): String? {
        return HceService.emulatedTagId.value
    }
    
    /**
     * Check if a specific key is currently being emulated
     */
    fun isEmulating(nfcKey: NfcKey): Boolean {
        return HceService.isEmulating.value && HceService.emulatedTagId.value == nfcKey.tagId
    }
}

sealed class EmulationResult {
    data class Success(val message: String) : EmulationResult()
    data class Error(val message: String) : EmulationResult()
}
