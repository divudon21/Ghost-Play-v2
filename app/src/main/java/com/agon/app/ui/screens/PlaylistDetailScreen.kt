package com.agon.app.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Serializable
data class PlaylistItem(
    val title: String, 
    val logoUrl: String, 
    val url: String,
    val group: String = "",
    val licenseType: String = "",
    val licenseKey: String = "",
    val userAgent: String = "",
    val cookie: String = ""
)

object FavoriteStorage {
    private const val PREFS_NAME = "favorites_prefs"
    private const val KEY = "favorites_json"

    fun saveFavorites(context: Context, favorites: List<PlaylistItem>) {
        val json = Json.encodeToString(favorites)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY, json).apply()
    }

    fun getFavorites(context: Context): List<PlaylistItem> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY, null)
        return if (json != null) {
            try { Json.decodeFromString(json) } catch (e: Exception) { emptyList() }
        } else emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlistUrl: String, 
    userAgent: String, 
    navController: NavController,
    initialShowFavorites: Boolean = false
) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<PlaylistItem>>(emptyList()) }
    var favorites by remember { mutableStateOf(FavoriteStorage.getFavorites(context)) }
    var isLoading by remember { mutableStateOf(true) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showFavoritesOnly by remember { mutableStateOf(initialShowFavorites) }
    var selectedItemForDialog by remember { mutableStateOf<PlaylistItem?>(null) }

    LaunchedEffect(playlistUrl) {
        items = fetchPlaylist(playlistUrl, userAgent)
        isLoading = false
    }

    val sourceItems = if (showFavoritesOnly) favorites else items
    val filteredItems = if (searchQuery.isBlank()) sourceItems else sourceItems.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(if (showFavoritesOnly) "Favorites" else "Playlist Items")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (showFavoritesOnly) {
                            showFavoritesOnly = false
                        } else {
                            navController.popBackStack() 
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFavoritesOnly = !showFavoritesOnly }) {
                        Icon(
                            imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorites"
                        )
                    }
                    if (isSearchActive) {
                        IconButton(onClick = { 
                            isSearchActive = false
                            searchQuery = "" 
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search")
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading && !showFavoritesOnly) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (filteredItems.isEmpty()) {
                Text(
                    text = if (showFavoritesOnly) "No favorites yet." else "No items found.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.8f)
                                .combinedClickable(
                                    onClick = {
                                        val encodedUrl = URLEncoder.encode(item.url, "UTF-8")
                                        var route = "player/$encodedUrl?"
                                        val params = mutableListOf<String>()
                                        if (item.userAgent.isNotBlank()) params.add("userAgent=${URLEncoder.encode(item.userAgent, "UTF-8")}")
                                        if (item.cookie.isNotBlank()) params.add("cookie=${URLEncoder.encode(item.cookie, "UTF-8")}")
                                        if (item.licenseType.isNotBlank()) params.add("licenseType=${URLEncoder.encode(item.licenseType, "UTF-8")}")
                                        if (item.licenseKey.isNotBlank()) params.add("licenseKey=${URLEncoder.encode(item.licenseKey, "UTF-8")}")
                                        
                                        // Pass the current state of showFavoritesOnly so we return to the same state
                                        params.add("showFavorites=$showFavoritesOnly")
                                        
                                        if (params.isNotEmpty()) {
                                            route += params.joinToString("&")
                                        } else {
                                            route = "player/$encodedUrl"
                                        }
                                        navController.navigate(route)
                                    },
                                    onLongClick = {
                                        selectedItemForDialog = item
                                    }
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (item.logoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = item.logoUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(4.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = item.title.ifBlank { "Unknown" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            selectedItemForDialog?.let { item ->
                val isFavorite = favorites.any { it.url == item.url }
                AlertDialog(
                    onDismissRequest = { selectedItemForDialog = null },
                    title = { Text(if (isFavorite) "Remove from Favorites?" else "Add to Favorites?") },
                    text = { Text(item.title) },
                    confirmButton = {
                        TextButton(onClick = {
                            val newFavorites = if (isFavorite) {
                                favorites.filter { it.url != item.url }
                            } else {
                                favorites + item
                            }
                            favorites = newFavorites
                            FavoriteStorage.saveFavorites(context, newFavorites)
                            selectedItemForDialog = null
                        }) {
                            Text(if (isFavorite) "Remove" else "Add")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedItemForDialog = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

suspend fun fetchPlaylist(url: String, userAgent: String): List<PlaylistItem> = withContext(Dispatchers.IO) {
    val items = mutableListOf<PlaylistItem>()
    try {
        val connection = URL(url).openConnection() as HttpURLConnection
        if (userAgent.isNotBlank() && userAgent != "Custom") {
            val realUa = when(userAgent) {
                "Chrome (PC)" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                "Chrome (Android)" -> "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                "Firefox (PC)" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0"
                "IE (PC)" -> "Mozilla/5.0 (Windows NT 10.0; Trident/7.0; rv:11.0) like Gecko"
                "iPhone" -> "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
                "Nokia" -> "Mozilla/5.0 (Symbian/3; Series60/5.2 NokiaN8-00/012.002; Profile/MIDP-2.1 Configuration/CLDC-1.1 ) AppleWebKit/533.4 (KHTML, like Gecko) NokiaBrowser/7.3.0 Mobile Safari/533.4 3gpp-gba"
                else -> userAgent
            }
            connection.setRequestProperty("User-Agent", realUa)
        } else if (userAgent == "Custom") {
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        } else {
            connection.setRequestProperty("User-Agent", userAgent)
        }
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        
        var redirect = false
        var status = connection.responseCode
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
                redirect = true
            }
        }
        
        val finalConnection = if (redirect) {
            val newUrl = connection.getHeaderField("Location")
            val newConn = URL(newUrl).openConnection() as HttpURLConnection
            if (userAgent.isNotBlank() && userAgent != "Custom") {
                newConn.setRequestProperty("User-Agent", connection.getRequestProperty("User-Agent"))
            }
            newConn
        } else {
            connection
        }
        
        val reader = BufferedReader(InputStreamReader(finalConnection.inputStream))
        var line: String?
        var currentTitle = ""
        var currentLogo = ""
        var currentGroup = ""
        var currentLicenseType = ""
        var currentLicenseKey = ""
        var currentUserAgent = ""
        var currentCookie = ""
        
        while (reader.readLine().also { line = it } != null) {
            val l = line!!.trim()
            if (l.startsWith("#KODIPROP:inputstream.adaptive.license_type=")) {
                currentLicenseType = l.substringAfter("=").trim()
            } else if (l.startsWith("#KODIPROP:inputstream.adaptive.license_key=")) {
                currentLicenseKey = l.substringAfter("=").trim()
            } else if (l.startsWith("#EXTVLCOPT:http-user-agent=")) {
                currentUserAgent = l.substringAfter("=").trim()
            } else if (l.startsWith("#EXTVLCOPT:http-cookie=")) {
                currentCookie = l.substringAfter("=").trim()
            } else if (l.startsWith("#EXTHTTP:")) {
                try {
                    val jsonStr = l.substringAfter(":")
                    if (jsonStr.contains("\"cookie\"")) {
                        val cookieMatch = "\"cookie\"\\s*:\\s*\"([^\"]+)\"".toRegex(RegexOption.IGNORE_CASE).find(jsonStr)
                        cookieMatch?.let { currentCookie = it.groupValues[1] }
                    }
                    if (jsonStr.contains("\"user-agent\"")) {
                        val uaMatch = "\"user-agent\"\\s*:\\s*\"([^\"]+)\"".toRegex(RegexOption.IGNORE_CASE).find(jsonStr)
                        uaMatch?.let { currentUserAgent = it.groupValues[1] }
                    }
                } catch (e: Exception) {}
            } else if (l.startsWith("#EXTINF")) {
                val logoMatch = Regex("tvg-logo=\"([^\"]+)\"").find(l)
                currentLogo = logoMatch?.groupValues?.get(1) ?: ""
                
                val groupMatch = Regex("group-title=\"([^\"]+)\"").find(l)
                currentGroup = groupMatch?.groupValues?.get(1) ?: ""
                
                val nameParts = l.split(",")
                if (nameParts.size > 1) {
                    currentTitle = nameParts.subList(1, nameParts.size).joinToString(",").trim()
                } else {
                    currentTitle = "Unknown Channel"
                }
            } else if (l.isNotEmpty() && !l.startsWith("#") && currentTitle.isNotEmpty()) {
                var streamUrl = l
                var urlUserAgent = currentUserAgent
                var urlCookie = currentCookie
                
                val delimiter = if (streamUrl.contains("|")) "|" else if (streamUrl.contains("%7C", ignoreCase = true)) "%7C" else null
                
                if (delimiter != null) {
                    val parts = streamUrl.split(Regex(delimiter, RegexOption.IGNORE_CASE), limit = 2)
                    streamUrl = parts[0]
                    if (parts.size > 1) {
                        val headerString = parts[1]
                        val headerParts = headerString.split("&")
                        for (hp in headerParts) {
                            val kv = hp.split("=", limit = 2)
                            if (kv.size == 2) {
                                if (kv[0].equals("User-Agent", ignoreCase = true)) {
                                    urlUserAgent = kv[1]
                                } else if (kv[0].equals("cookie", ignoreCase = true)) {
                                    urlCookie = kv[1]
                                }
                            }
                        }
                    }
                }
                
                items.add(PlaylistItem(
                    title = currentTitle.ifBlank { "Unknown Channel" }, 
                    logoUrl = currentLogo, 
                    url = streamUrl,
                    group = currentGroup,
                    licenseType = currentLicenseType,
                    licenseKey = currentLicenseKey,
                    userAgent = urlUserAgent,
                    cookie = urlCookie
                ))
                
                currentTitle = ""
                currentLogo = ""
                currentGroup = ""
                currentLicenseType = ""
                currentLicenseKey = ""
                currentUserAgent = ""
                currentCookie = ""
            }
        }
        reader.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    items
}