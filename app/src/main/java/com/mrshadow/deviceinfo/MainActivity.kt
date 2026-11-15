package com.mrshadow.deviceinfo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mrshadow.deviceinfo.device.DeviceInfoService
import com.mrshadow.deviceinfo.device.InfoSection
import com.mrshadow.deviceinfo.device.asReport
import java.util.Locale

private enum class Page { Home, Apps, Developer, Settings }

private val neonGreen = Color(0xFF9BEA45)
private val deepBlack = Color(0xFF101110)
private val deepGray = Color(0xFF171B17)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShadowApp() }
    }
}

@Composable
private fun ShadowApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }

    var page by remember { mutableStateOf(Page.Home) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
    var lang by remember { mutableStateOf(prefs.getString("language", "en") ?: "en") }
    var showUnavailable by remember { mutableStateOf(prefs.getBoolean("show_unavailable", true)) }
    var notifications by remember { mutableStateOf(prefs.getBoolean("notifications", false)) }

    val colors = if (dark) {
        darkColorScheme(
            primary = neonGreen,
            background = deepBlack,
            surface = deepGray
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF5D8E21),
            background = Color(0xFFf4f6f5),
            surface = Color(0xFFf2f4f2)
        )
    }

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "MR SHADOW DEVICE INFO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    val items = listOf(
                        Pair(Page.Home, if (lang == "bn") "হোম" else "Home"),
                        Pair(Page.Apps, if (lang == "bn") "অ্যাপস" else "Apps"),
                        Pair(Page.Developer, if (lang == "bn") "ডেভেলপার" else "Developer"),
                        Pair(Page.Settings, if (lang == "bn") "সেটিংস" else "Settings")
                    )

                    items.forEach { item ->
                        val icon = when (item.first) {
                            Page.Home -> Icons.Default.Home
                            Page.Apps -> Icons.Default.Apps
                            Page.Developer -> Icons.Default.Person
                            Page.Settings -> Icons.Default.Settings
                        }

                        NavigationBarItem(
                            selected = page == item.first,
                            onClick = { page = item.first },
                            icon = { Icon(icon, contentDescription = item.second) },
                            label = { Text(item.second) }
                        )
                    }
                }
            }
        ) { padding ->
            when (page) {
                Page.Home -> HomeScreen(showUnavailable, Modifier.padding(padding), context)
                Page.Apps -> AppsScreen(Modifier.padding(padding), context)
                Page.Developer -> DeveloperScreen(Modifier.padding(padding), context)
                Page.Settings -> SettingsScreen(
                    dark = dark,
                    setDark = { dark = it; prefs.edit().putBoolean("dark", it).apply() },
                    language = lang,
                    setLanguage = { lang = it; prefs.edit().putString("language", it).apply() },
                    showUnavailable = showUnavailable,
                    setShowUnavailable = { showUnavailable = it; prefs.edit().putBoolean("show_unavailable", it).apply() },
                    notifications = notifications,
                    setNotifications = { notifications = it; prefs.edit().putBoolean("notifications", it).apply() },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(showUnavailable: Boolean, modifier: Modifier, context: Context) {
    val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)

    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(DeviceInfoService(context).collect().asReport())
                }
            }
        }
    }

    val fields = remember {
        mutableStateOf(DeviceInfoService(context).collect())
    }

    fun refresh() {
        fields.value = DeviceInfoService(context).collect()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Your device", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Information available through standard Android APIs.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { refresh() }) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Refresh")
                }

                OutlinedButton(onClick = {
                    val report = fields.value.asReport()
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Device information", report))
                }) {
                    Icon(Icons.Default.ContentCopy, null)
                    Text("Copy")
                }

                OutlinedButton(onClick = { createFile.launch("device-info.txt") }) {
                    Icon(Icons.Default.Download, null)
                    Text("Export")
                }
            }

            OutlinedButton(
                onClick = { shareText(context, fields.value.asReport()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Share report")
            }
        }

        items(fields.value) { section ->
            InfoSectionCard(section, showUnavailable)
        }
    }
}

@Composable
private fun InfoSectionCard(section: InfoSection, showUnavailable: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val visibleFields = if (showUnavailable) section.fields else section.fields.filterNot { it.value == "Not Available" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(section.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) "Collapse" else "Expand")
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                visibleFields.forEach { field ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(field.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(field.value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AppsScreen(modifier: Modifier, context: Context) {
    val apps = remember {
        AppsRepository(context).load()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Developer apps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("App links are loaded from the local apps.json configuration.")
        }

        if (apps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Icon(Icons.Default.Apps, null)
                        Spacer(Modifier.height(8.dp))
                        Text("No apps have been published yet.")
                        Text("Add verified app details and real HTTPS links to the local JSON catalog.")
                    }
                }
            }
        }

        items(apps, key = { it.id }) { app ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${app.category}${if (app.version != null) " • ${app.version}" else ""}",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(app.description)

                    val url = app.downloadUrl ?: app.websiteUrl
                    if (!url.isNullOrBlank()) {
                        Button(onClick = { openUrl(context, url) }) {
                            Text(if (app.downloadUrl != null) "Open / Download" else "Open website")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperScreen(modifier: Modifier, context: Context) {
    var preview by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(110.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(56.dp))
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("𝙼𝚁  𝚂𝙷𝙰𝙳𝙾𝚆", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Android Device Services\\nRoot Support\\nDevice Information Utility", style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.height(20.dp))
        ContactRow("WhatsApp", "https://wa.me/message/AXTGUAHKBLIHL1") { openUrl(context, it) }
        Spacer(Modifier.height(10.dp))
        ContactRow("Telegram", "https://t.me/mr_shadow23") { openUrl(context, it) }

        Spacer(Modifier.height(22.dp))
        Button(
            onClick = { preview = DeviceInfoService(context).collect().asReport() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Security, null)
            Spacer(Modifier.width(8.dp))
            Text("Review device report")
        }

        Text(
            "Nothing is sent unless you review and confirm the report.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    preview?.let { report ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text("Review device information") },
            text = {
                Column {
                    Text("This preview only shows the data and opens Android Sharesheet when you continue. No backend, bot, or token is configured.")
                    Spacer(Modifier.height(10.dp))
                    Text(report, Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    preview = null
                    shareText(context, report)
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { preview = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ContactRow(title: String, url: String, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(url) }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (title == "WhatsApp") Icons.Default.Chat else Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Icon(Icons.Default.OpenInNew, null)
        }
    }
}

@Composable
private fun SettingsScreen(
    dark: Boolean,
    setDark: (Boolean) -> Unit,
    language: String,
    setLanguage: (String) -> Unit,
    autoRefresh: Boolean,
    setAutoRefresh: (Boolean) -> Unit,
    showUnavailable: Boolean,
    setShowUnavailable: (Boolean) -> Unit,
    notifications: Boolean,
    setNotifications: (Boolean) -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        item {
            SettingRow("Appearance", "Dark mode", dark, setDark)
        }

        item {
            SettingRow("Device information", "Show unavailable fields", showUnavailable, setShowUnavailable)
        }

        item {
            SettingRow("Device information", "Auto refresh on app launch", autoRefresh, setAutoRefresh)
        }

        item {
            SettingRow("Notifications", "Enable notifications", notifications, setNotifications)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Language", fontWeight = FontWeight.SemiBold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("English", "বাংলা").forEach { label ->
                            TextButton(onClick = { setLanguage(label) }) {
                                Text(if (language == label) "✓ $label" else label)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Privacy & support", fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = {
                        val text = """
                            MR SHADOW DEVICE INFO
                            No IMEI, IMSI, serial number, phone number, SIM identifiers, location, account identifiers, credentials, or personal files are collected.
                            Device data stays on device until you choose to share it.
                            No backend or bot is configured.
                        """.trimIndent()
                        showDialog(context = LocalContext.current, message = text)
                    }) { Text("Privacy") }
                    TextButton(onClick = { shareText(LocalContext.current, "MR SHADOW DEVICE INFO") }) { Text("Share app") }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(group: String, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(group, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(title)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

private fun showDialog(context: Context, message: String) {
    val builder = androidx.appcompat.app.AlertDialog.Builder(context)
    builder.setTitle("Privacy")
    builder.setMessage(message)
    builder.show()
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, "Share device information"))
}

private fun openUrl(context: Context, url: String) {
    val uri = Uri.parse(url)
    if (uri.scheme == "https" && !uri.host.isNullOrBlank()) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}
