package com.example.nfckeyring

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nfckeyring.ui.KeyViewModel
import com.example.nfckeyring.util.SecurePrefs
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: KeyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            viewModel.allKeys.collect { keys ->
                // Observe keys; update UI as needed
            }
        }

        val prefs = SecurePrefs.getPrefs(this)
        if (!prefs.contains("initialized")) {
            prefs.edit().putBoolean("initialized", true).apply()
        }
    }
}
