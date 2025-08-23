package com.luvia.nfckeychain.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNdefDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }
    var isValueError by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    val types = listOf("URL", "Phone", "Email", "SMS", "Geo")
    var selectedType by remember { mutableStateOf(types.first()) }

    // Live validation states
    val nameValid by remember(name) { mutableStateOf(name.isNotBlank()) }
    val valueValid by remember(selectedType, value) {
        mutableStateOf(
            when (selectedType) {
                "URL" -> value.isNotBlank() && isValidHttpUrl(ensureHttpScheme(value))
                "Phone" -> value.isNotBlank() && isValidPhone(value)
                "Email" -> value.isNotBlank() && isValidEmail(value)
                "SMS" -> value.isNotBlank() && isValidPhone(value)
                "Geo" -> value.isNotBlank() && (parseGeo(value) != null)
                else -> false
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Create NDEF Tag",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        isNameError = false
                    },
                    label = { Text("Tag Name *") },
                    isError = isNameError,
                    supportingText = {
                        if (isNameError) {
                            Text("Name is required")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Type selector
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        types.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedType = option
                                    typeExpanded = false
                                    // reset validation on type change
                                    isValueError = false
                                    value = ""
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Value field (contextual)
                OutlinedTextField(
                    value = value,
                    onValueChange = { 
                        value = it
                        isValueError = false
                    },
                    label = { Text(contextualLabel(selectedType)) },
                    isError = (value.isNotBlank() && !valueValid) || isValueError,
                    supportingText = {
                        when {
                            isValueError || (value.isNotBlank() && !valueValid) -> Text(contextualError(selectedType))
                            value.isBlank() -> Text(contextualHint(selectedType))
                            else -> Text("Looks good")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            // Validate inputs
                            if (name.isBlank()) {
                                isNameError = true
                                return@Button
                            }
                            val built = buildNdefUri(selectedType, value)
                            if (built == null) {
                                isValueError = true
                                return@Button
                            }
                            onConfirm(name, built, description.takeIf { it.isNotBlank() })
                        },
                        enabled = nameValid && valueValid
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

private fun contextualLabel(type: String): String = when (type) {
    "URL" -> "URL *"
    "Phone" -> "Phone number *"
    "Email" -> "Email address *"
    "SMS" -> "Phone number *"
    "Geo" -> "Latitude,Longitude *"
    else -> "Value *"
}

private fun contextualHint(type: String): String = when (type) {
    "URL" -> "e.g., https://example.com"
    "Phone" -> "e.g., +18005551234"
    "Email" -> "e.g., user@example.com"
    "SMS" -> "e.g., +18005551234"
    "Geo" -> "e.g., 37.7749,-122.4194"
    else -> ""
}

private fun contextualError(type: String): String = when (type) {
    "URL" -> "Valid URL is required"
    "Phone" -> "Valid phone number is required"
    "Email" -> "Valid email address is required"
    "SMS" -> "Valid phone number is required"
    "Geo" -> "Valid coordinates are required"
    else -> "Value is required"
}

private fun buildNdefUri(type: String, raw: String): String? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    return when (type) {
        "URL" -> if (isValidHttpUrl(value)) ensureHttpScheme(value) else null
        "Phone" -> if (isValidPhone(value)) "tel:${value.replace(" ", "")}" else null
        "Email" -> if (isValidEmail(value)) "mailto:$value" else null
        "SMS" -> if (isValidPhone(value)) "sms:${value.replace(" ", "")}" else null
        "Geo" -> parseGeo(value)
        else -> null
    }
}

private fun isValidHttpUrl(url: String): Boolean = try {
    val u = java.net.URL(ensureHttpScheme(url))
    u.protocol == "http" || u.protocol == "https"
} catch (_: Exception) { false }

private fun ensureHttpScheme(url: String): String =
    if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"

private fun isValidPhone(s: String): Boolean {
    val digits = s.replace(" ", "")
    return digits.matches(Regex("^\\+?[0-9]{5,15}$"))
}

private fun isValidEmail(s: String): Boolean =
    s.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))

private fun parseGeo(s: String): String? {
    val parts = s.split(',').map { it.trim() }
    if (parts.size != 2) return null
    val lat = parts[0].toDoubleOrNull() ?: return null
    val lon = parts[1].toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    return "geo:$lat,$lon"
}
