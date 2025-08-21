package com.example.nfckeyring

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                val tagId = it.id.joinToString("") { b -> "%02X".format(b) }
                Toast.makeText(this, "Tag detected: $tagId", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "NFC Tag discovered: $tagId")
            }
        }
    }
}
