package com.luvia.nfckeychain.nfc.utils

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.nio.charset.StandardCharsets

object NdefUtils {
    
    /**
     * Create an NDEF message containing a URL record
     */
    fun createUrlMessage(url: String): NdefMessage {
        val urlRecord = createUrlRecord(url)
        return NdefMessage(arrayOf(urlRecord))
    }
    
    /**
     * Create an NDEF message that's properly formatted for IsoDep emulation
     */
    fun createIsoDepUrlMessage(url: String): ByteArray {
        val ndefMessage = createUrlMessage(url)
        val messageBytes = ndefMessage.toByteArray()
        
        // For IsoDep, we need to wrap the NDEF message in a specific format
        // that includes the message length and proper TLV structure
        val tlvLength = messageBytes.size
        val response = ByteArray(2 + tlvLength)
        response[0] = 0x00.toByte() // TLV tag for NDEF message
        response[1] = tlvLength.toByte() // Length
        System.arraycopy(messageBytes, 0, response, 2, tlvLength)
        
        return response
    }
    
    /**
     * Create an NDEF URL record
     */
    private fun createUrlRecord(url: String): NdefRecord {
        // Remove protocol prefix if present for shorter encoding
        val urlToEncode = if (url.startsWith("https://")) {
            url.substring(8) // Remove "https://"
        } else if (url.startsWith("http://")) {
            url.substring(7) // Remove "http://"
        } else {
            url
        }
        
        // Determine the protocol prefix byte
        val prefixByte = when {
            url.startsWith("https://") -> 0x02 // "https://"
            url.startsWith("http://") -> 0x01  // "http://"
            else -> 0x00 // No prefix
        }
        
        // Create the payload: [prefix byte][url bytes]
        val urlBytes = urlToEncode.toByteArray(StandardCharsets.UTF_8)
        val payload = ByteArray(1 + urlBytes.size)
        payload[0] = prefixByte.toByte()
        System.arraycopy(urlBytes, 0, payload, 1, urlBytes.size)
        
        return NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_URI,
            ByteArray(0), // No ID
            payload
        )
    }
    
    /**
     * Convert NDEF message to byte array for storage/emulation
     */
    fun ndefMessageToBytes(ndefMessage: NdefMessage): ByteArray {
        return ndefMessage.toByteArray()
    }
    
    /**
     * Create a byte array representation of a URL NDEF message
     */
    fun createUrlMessageBytes(url: String): ByteArray {
        val ndefMessage = createUrlMessage(url)
        val bytes = ndefMessageToBytes(ndefMessage)
        println("DEBUG: Created NDEF message for URL '$url': ${bytesToHex(bytes)}")
        return bytes
    }
    
    /**
     * Convert byte array to hex string for debugging
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789ABCDEF"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }
    
    /**
     * Parse URL from NDEF record (for reading existing tags)
     */
    fun parseUrlFromRecord(record: NdefRecord): String? {
        if (record.tnf != NdefRecord.TNF_WELL_KNOWN || 
            !record.type.contentEquals(NdefRecord.RTD_URI)) {
            return null
        }
        
        val payload = record.payload
        if (payload.isEmpty()) return null
        
        val prefixByte = payload[0]
        val urlBytes = payload.copyOfRange(1, payload.size)
        val urlString = String(urlBytes, StandardCharsets.UTF_8)
        
        return when (prefixByte.toInt()) {
            0x01 -> "http://$urlString"
            0x02 -> "https://$urlString"
            else -> urlString
        }
    }
}
