package com.agon.app.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder

@Serializable
data class Playlist(val name: String, val url: String, val userAgent: String)

object PlaylistStorage {
    private const val PREFS_NAME = "playlists_prefs"
    private const val KEY = "playlists_json"

    fun savePlaylists(context: Context, playlists: List<Playlist>) {
        val json = Json.encodeToString(playlists)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY, json).apply()
    }

    fun getPlaylists(context: Context): List<Playlist> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY, null)
        return if (json != null) {
            try { Json.decodeFromString(json) } catch (e: Exception) { emptyList() }
        } else emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(navController: NavController) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var playlists by remember { mutableStateOf(PlaylistStorage.getPlaylists(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlists") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Playlist")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (playlists.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No playlists found.",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Click the + icon to add one!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlists) { playlist ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val encodedUrl = URLEncoder.encode(playlist.url, "UTF-8")
                                    val encodedUa = URLEncoder.encode(playlist.userAgent, "UTF-8")
                                    navController.navigate("playlist_detail/$encodedUrl?userAgent=$encodedUa")
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = playlist.name, style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = playlist.url, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "User Agent: ${playlist.userAgent}", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddPlaylistDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, url, userAgent ->
                    val newList = playlists + Playlist(name, url, userAgent)
                    PlaylistStorage.savePlaylists(context, newList)
                    playlists = newList
                    showAddDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaylistDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    val userAgents = listOf(
        "Chrome (PC)", 
        "Chrome (Android)", 
        "Firefox (PC)", 
        "IE (PC)", 
        "iPhone", 
        "Nokia", 
        "Custom"
    )
    var selectedAgent by remember { mutableStateOf(userAgents[0]) }
    var customAgent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Playlist") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedAgent,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("User Agent") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        userAgents.forEach { agent ->
                            DropdownMenuItem(
                                text = { Text(agent) },
                                onClick = {
                                    selectedAgent = agent
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                if (selectedAgent == "Custom") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customAgent,
                        onValueChange = { customAgent = it },
                        label = { Text("Custom User Agent") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val finalAgent = if (selectedAgent == "Custom") customAgent else selectedAgent
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onSave(name, url, finalAgent)
                    }
                },
                enabled = name.isNotBlank() && url.isNotBlank() && (selectedAgent != "Custom" || customAgent.isNotBlank())
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
