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
    var url by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }
    var isUrlError by remember { mutableStateOf(false) }

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
                
                // URL field
                OutlinedTextField(
                    value = url,
                    onValueChange = { 
                        url = it
                        isUrlError = false
                    },
                    label = { Text("URL *") },
                    isError = isUrlError,
                    supportingText = {
                        if (isUrlError) {
                            Text("Valid URL is required")
                        } else {
                            Text("e.g., https://example.com")
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
                            
                            if (url.isBlank() || !isValidUrl(url)) {
                                isUrlError = true
                                return@Button
                            }
                            
                            onConfirm(name, url, description.takeIf { it.isNotBlank() })
                        }
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

private fun isValidUrl(url: String): Boolean {
    return try {
        val urlObj = java.net.URL(url)
        urlObj.protocol in listOf("http", "https")
    } catch (e: Exception) {
        false
    }
}
