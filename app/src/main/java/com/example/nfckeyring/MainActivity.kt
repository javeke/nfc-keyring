package com.example.nfckeyring

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.Ndef
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nfckeyring.ui.KeyViewModel
import com.example.nfckeyring.util.BiometricAuth
import com.example.nfckeyring.util.SecurePrefs
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: KeyViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null
    private var nfcPendingIntent: PendingIntent? = null
    private lateinit var nfcIntentFilters: Array<IntentFilter>
    private lateinit var tagUidTextView: TextView
    private lateinit var formattedTextView: TextView
    private lateinit var rawHexTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tagUidTextView = findViewById(R.id.tagUidTextView)
        formattedTextView = findViewById(R.id.formattedTextView)
        rawHexTextView = findViewById(R.id.rawHexTextView)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        nfcPendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val tagDetected = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        nfcIntentFilters = arrayOf(tagDetected)

        BiometricAuth.authenticate(this, onSuccess = {
            viewModel.initialize()
            lifecycleScope.launch {
                viewModel.allKeys.collect { keys ->
                    // Observe keys; update UI as needed
                }
            }
            val prefs = SecurePrefs.getPrefs(this)
            if (!prefs.contains("initialized")) {
                prefs.edit().putBoolean("initialized", true).apply()
            }
        }, onError = {
            finish()
        })
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, nfcIntentFilters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let {
                val tagId = it.id.toHexString()
                tagUidTextView.text = getString(R.string.tag_uid_label) + " " + tagId
                Toast.makeText(this, "Tag detected: $tagId", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "NFC Tag discovered: $tagId")

                val ndef = Ndef.get(it)
                if (ndef != null) {
                    val message = ndef.cachedNdefMessage
                    if (message != null) {
                        displayNdefMessage(message)
                    } else {
                        formattedTextView.text = getString(R.string.no_ndef)
                        rawHexTextView.text = ""
                    }
                } else {
                    formattedTextView.text = getString(R.string.no_ndef)
                    rawHexTextView.text = ""
                }
            }
        }
    }

    private fun displayNdefMessage(message: NdefMessage) {
        val formatted = StringBuilder()
        val raw = StringBuilder()
        for (record in message.records) {
            raw.append(record.payload.toHexString()).append('\n')
            formatted.append(
                when {
                    record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT) ->
                        "Text: ${parseTextRecord(record.payload)}"
                    record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI) ->
                        "URI: ${record.toUri()}"
                    record.tnf == NdefRecord.TNF_MIME_MEDIA && record.type.contentEquals("application/vnd.wfa.wsc".toByteArray()) ->
                        "WiFi: ${parseWifiRecord(record.payload)}"
                    else -> "Unknown record"
                }
            ).append('\n')
        }
        formattedTextView.text = formatted.toString().trim()
        rawHexTextView.text = raw.toString().trim()
    }

    private fun parseTextRecord(payload: ByteArray): String {
        val status = payload[0].toInt()
        val isUtf16 = status and 0x80 != 0
        val languageLength = status and 0x3F
        val textEncoding = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
        return String(payload, 1 + languageLength, payload.size - 1 - languageLength, textEncoding)
    }

    private fun parseWifiRecord(payload: ByteArray): String {
        var index = 0
        val builder = StringBuilder()
        while (index + 4 <= payload.size) {
            val type = ((payload[index].toInt() and 0xFF) shl 8) or (payload[index + 1].toInt() and 0xFF)
            val length = ((payload[index + 2].toInt() and 0xFF) shl 8) or (payload[index + 3].toInt() and 0xFF)
            if (index + 4 + length > payload.size) break
            val value = payload.copyOfRange(index + 4, index + 4 + length)
            when (type) {
                0x1045 -> builder.append("SSID: ").append(String(value)).append(' ')
                0x1027 -> builder.append("Password: ").append(String(value)).append(' ')
            }
            index += 4 + length
        }
        val result = builder.toString().trim()
        return if (result.isNotEmpty()) result else "WiFi config data"
    }

    private fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }
}
