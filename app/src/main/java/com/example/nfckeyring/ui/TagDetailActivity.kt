package com.example.nfckeyring.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.nfckeyring.R
import com.example.nfckeyring.data.TagEntity

class TagDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_detail)

        val tag = intent.getSerializableExtra("tag") as? TagEntity
        val labelView = findViewById<TextView>(R.id.tagLabelTextView)
        val formattedView = findViewById<TextView>(R.id.formattedTextView)
        val rawView = findViewById<TextView>(R.id.rawHexTextView)
        val emulateButton = findViewById<Button>(R.id.emulateButton)

        tag?.let {
            labelView.text = it.label
            formattedView.text = formatPayload(it.payload)
            rawView.text = it.payload
            emulateButton.setOnClickListener { _ ->
                Toast.makeText(this, getString(R.string.emulation_started, it.label), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatPayload(payload: String): String {
        return try {
            val bytes = payload.split(" ")
                .filter { it.isNotBlank() }
                .map { it.toInt(16).toByte() }
                .toByteArray()
            if (bytes.isNotEmpty()) {
                val status = bytes[0].toInt()
                val isUtf16 = status and 0x80 != 0
                val languageLength = status and 0x3F
                val textEncoding = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
                String(bytes, 1 + languageLength, bytes.size - 1 - languageLength, textEncoding)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
