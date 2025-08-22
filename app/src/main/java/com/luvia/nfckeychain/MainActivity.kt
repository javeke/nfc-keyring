package com.luvia.nfckeychain

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luvia.nfckeychain.data.model.NfcTagInfo
import com.luvia.nfckeychain.nfc.utils.NfcUtils
import com.luvia.nfckeychain.presentation.theme.NFCKeychainTheme
import com.luvia.nfckeychain.presentation.ui.components.AddKeyDialog
import com.luvia.nfckeychain.presentation.ui.components.KeyCard
import com.luvia.nfckeychain.presentation.ui.screens.AuthenticationScreen
import com.luvia.nfckeychain.presentation.viewmodel.KeyViewModel

class MainActivity : FragmentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var intentFiltersArray: Array<IntentFilter>? = null
    private var techListsArray: Array<Array<String>>? = null
    
    private val viewModel: KeyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        // Create PendingIntent for NFC
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        
        // Create intent filters for NFC
        val ndef = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        try {
            ndef.addDataType("*/*")
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("fail", e)
        }
        intentFiltersArray = arrayOf(ndef)
        techListsArray = arrayOf(arrayOf(Ndef::class.java.name))
        
        setContent {
            NFCKeychainTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NFCKeychainApp(
                        viewModel = viewModel,
                        onAuthenticate = { viewModel.authenticate(this) }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Only enable NFC if authenticated
        if (viewModel.isAuthenticated.value) {
            nfcAdapter?.enableForegroundDispatch(
                this, pendingIntent, intentFiltersArray, techListsArray
            )
        }
    }
    
    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        println("DEBUG: onNewIntent called, action: ${intent.action}")
        println("DEBUG: isAuthenticated: ${viewModel.isAuthenticated.value}")
        
        // Only handle NFC if authenticated
        if (!viewModel.isAuthenticated.value) {
            println("DEBUG: Not authenticated, returning early")
            return
        }
        
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            
            println("DEBUG: NFC intent detected, handling tag")
            val tag: Tag? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            tag?.let { handleNfcTag(it) }
        }
    }
    
    private fun handleNfcTag(tag: Tag) {
        println("DEBUG: handleNfcTag called")
        val tagInfo = NfcUtils.getTagInfo(tag)
        val ndefMessage = NfcUtils.readNdefMessage(tag)
        
        // Update the tag info with NDEF message
        val updatedTagInfo = tagInfo.copy(ndefMessage = ndefMessage)
        
        // Check if this tag is already stored
        val existingKey = viewModel.getKeyByTagId(tagInfo.tagId)
        
        if (existingKey != null) {
            // Tag already exists - show existing key info
            println("Existing key found: ${existingKey.name}")
            // TODO: Show key details or quick access
        } else {
            // New tag - store it for potential key creation
            println("DEBUG: Setting last scanned tag")
            viewModel.setLastScannedTag(updatedTagInfo)
            println("New NFC Tag detected: ${tagInfo.tagId}")
            println("Tag type: ${tagInfo.tagType}")
            println("NDEF Message: $ndefMessage")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCKeychainApp(
    viewModel: KeyViewModel,
    onAuthenticate: () -> Unit
) {
    val keys by viewModel.keys.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val lastScannedTag by viewModel.lastScannedTag.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val biometricAvailable by viewModel.biometricAvailable.collectAsStateWithLifecycle()
    val isEmulating by viewModel.isEmulating.collectAsStateWithLifecycle()
    val emulatedTagId by viewModel.emulatedTagId.collectAsStateWithLifecycle()
    val emulationMessage by viewModel.emulationMessage.collectAsStateWithLifecycle()
    
    var showAddKeyDialog by remember { mutableStateOf(false) }
    var isNfcEnabled by remember { mutableStateOf(true) }
    
    // Show authentication screen if not authenticated
    if (!isAuthenticated) {
        AuthenticationScreen(
            isAuthenticating = isAuthenticating,
            biometricAvailable = biometricAvailable,
            onAuthenticate = onAuthenticate
        )
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NFC Keychain") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // Lock button
                    IconButton(
                        onClick = { viewModel.lockApp() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "Lock App"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (lastScannedTag != null) {
                FloatingActionButton(
                    onClick = { 
                        showAddKeyDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Key")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // NFC Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNfcEnabled) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = "NFC Status",
                        tint = if (isNfcEnabled) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isNfcEnabled) "NFC Ready" else "NFC Disabled",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isNfcEnabled) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (lastScannedTag != null) 
                                "Tap + to add the scanned tag" 
                            else if (isNfcEnabled) 
                                "Tap an NFC tag to read it" 
                            else 
                                "Enable NFC in settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isNfcEnabled) 
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                            else 
                                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Emulation Status Card
            if (isEmulating && emulatedTagId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Emulating",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Emulation Active",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Hold near NFC reader to emulate tag",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        TextButton(
                            onClick = { viewModel.stopEmulation() }
                        ) {
                            Text("Stop")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Keys List
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (keys.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "No Keys",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Keys Stored",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap an NFC tag, then tap the + button to add your first key",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(keys) { key ->
                        KeyCard(
                            key = key,
                            onEdit = {
                                // TODO: Implement edit functionality
                                println("Edit key: ${key.name}")
                            },
                            onDelete = {
                                viewModel.deleteKey(key)
                            },
                            onToggleStatus = {
                                viewModel.toggleKeyStatus(key)
                            },
                            onEmulate = {
                                if (viewModel.isKeyBeingEmulated(key)) {
                                    viewModel.stopEmulation()
                                } else {
                                    viewModel.startEmulation(key)
                                }
                            },
                            isEmulating = viewModel.isKeyBeingEmulated(key),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
        
        // Add Key Dialog
        if (showAddKeyDialog) {
            AddKeyDialog(
                tagInfo = lastScannedTag,
                onDismiss = {
                    showAddKeyDialog = false
                    viewModel.clearLastScannedTag()
                },
                onConfirm = { name, description, data ->
                    lastScannedTag?.let { tagInfo ->
                        viewModel.addKey(name, description, tagInfo, data)
                    }
                    showAddKeyDialog = false
                    viewModel.clearLastScannedTag()
                }
            )
        }
    }
}
