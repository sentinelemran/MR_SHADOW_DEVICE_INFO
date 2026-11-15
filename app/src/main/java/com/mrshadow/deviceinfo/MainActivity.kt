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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mrshadow.deviceinfo.data.AppsRepository
import com.mrshadow.deviceinfo.device.DeviceInfoService
import com.mrshadow.deviceinfo.device.InfoSection
import com.mrshadow.deviceinfo.device.asReport

private enum class Page { HOME, APPS, DEVELOPER, SETTINGS }
private enum class Language { EN, BN }
private val neonGreen = Color(0xFF9BEA45)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShadowApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShadowApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf(Page.HOME) }
    var darkTheme by remember { mutableStateOf(preferences.getBoolean("dark", true)) }
    var language by remember {
        mutableStateOf(if (preferences.getString("language", "en") == "bn") Language.BN else Language.EN)
    }
    var showUnavailable by remember { mutableStateOf(preferences.getBoolean("show_unavailable", true)) }
    var autoRefresh by remember { mutableStateOf(preferences.getBoolean("auto_refresh", true)) }
    var notifications by remember { mutableStateOf(preferences.getBoolean("notifications", false)) }
    var deviceSections by remember { mutableStateOf(emptyList<InfoSection>()) }

    fun refreshDeviceInfo() {
        deviceSections = runCatching { DeviceInfoService(context).collect() }.getOrDefault(emptyList())
    }

    LaunchedEffect(autoRefresh) {
        if (autoRefresh || deviceSections.isEmpty()) refreshDeviceInfo()
    }

    val colors = if (darkTheme) {
        darkColorScheme(primary = neonGreen, background = Color(0xFF080A08), surface = Color(0xFF151815))
    } else {
        lightColorScheme(primary = Color(0xFF426B13))
    }

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("MR SHADOW DEVICE INFO", fontWeight = FontWeight.Bold) }
                )
            },
            bottomBar = {
                NavigationBar {
                    val pages = listOf(
                        Page.HOME to tr(language, "Home", "হোম"),
                        Page.APPS to tr(language, "Apps", "অ্যাপস"),
                        Page.DEVELOPER to tr(language, "Developer", "ডেভেলপার"),
                        Page.SETTINGS to tr(language, "Settings", "সেটিংস")
                    )
                    pages.forEach { (item, label) ->
                        val icon = when (item) {
                            Page.HOME -> Icons.Default.Home
                            Page.APPS -> Icons.Default.Apps
                            Page.DEVELOPER -> Icons.Default.Person
                            Page.SETTINGS -> Icons.Default.Settings
                        }
                        NavigationBarItem(
                            selected = page == item,
                            onClick = { page = item },
                            icon = { androidx.compose.material3.Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            when (page) {
                Page.HOME -> HomeScreen(language, deviceSections, showUnavailable, ::refreshDeviceInfo, Modifier.padding(padding))
                Page.APPS -> AppsScreen(language, Modifier.padding(padding))
                Page.DEVELOPER -> DeveloperScreen(language, deviceSections, Modifier.padding(padding))
                Page.SETTINGS -> SettingsScreen(
                    language = language,
                    darkTheme = darkTheme,
                    onDarkThemeChange = {
                        darkTheme = it
                        preferences.edit().putBoolean("dark", it).apply()
                    },
                    onLanguageChange = {
                        language = it
                        preferences.edit().putString("language", if (it == Language.BN) "bn" else "en").apply()
                    },
                    showUnavailable = showUnavailable,
                    onShowUnavailableChange = {
                        showUnavailable = it
                        preferences.edit().putBoolean("show_unavailable", it).apply()
                    },
                    autoRefresh = autoRefresh,
                    onAutoRefreshChange = {
                        autoRefresh = it
                        preferences.edit().putBoolean("auto_refresh", it).apply()
                    },
                    notifications = notifications,
                    onNotificationsChange = {
                        notifications = it
                        preferences.edit().putBoolean("notifications", it).apply()
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    language: Language,
    sections: List<InfoSection>,
    showUnavailable: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(sections.asReport()) }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(tr(language, "Your device", "আপনার ডিভাইস"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(tr(language, "Information is read locally using standard Android APIs.", "স্ট্যান্ডার্ড Android API দিয়ে তথ্য শুধু ডিভাইসেই দেখা হয়।"))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onRefresh) {
                    androidx.compose.material3.Icon(Icons.Default.Refresh, null)
                    Text(tr(language, "Refresh", "রিফ্রেশ"))
                }
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Device information", sections.asReport()))
                }) {
                    androidx.compose.material3.Icon(Icons.Default.ContentCopy, null)
                    Text(tr(language, "Copy", "কপি"))
                }
                OutlinedButton(onClick = { exporter.launch("device-info.txt") }) {
                    androidx.compose.material3.Icon(Icons.Default.Download, null)
                    Text("TXT")
                }
            }
            OutlinedButton(
                onClick = { shareText(context, sections.asReport(), "Share device information") },
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.material3.Icon(Icons.Default.Share, null)
                Text(tr(language, "Share report", "রিপোর্ট শেয়ার"))
            }
        }
        if (sections.isEmpty()) item { Text(tr(language, "Not Available", "পাওয়া যায়নি")) }
        items(sections) { section -> InfoSectionCard(section, showUnavailable) }
    }
}

@Composable
private fun InfoSectionCard(section: InfoSection, showUnavailable: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val fields = if (showUnavailable) section.fields else section.fields.filterNot { it.value == "Not Available" }
    Card(Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(section.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                androidx.compose.material3.Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                if (fields.isEmpty()) Text("Not Available")
                fields.forEach { field ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(field.label, Modifier.weight(1f))
                        Text(field.value, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AppsScreen(language: Language, modifier: Modifier) {
    val context = LocalContext.current
    val apps = remember { AppsRepository(context).load() }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(tr(language, "Developer-managed apps", "ডেভেলপার পরিচালিত অ্যাপ"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        if (apps.isEmpty()) item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.Apps, null)
                    Text(tr(language, "No apps have been listed yet.", "এখনও কোনো অ্যাপ তালিকাভুক্ত করা হয়নি।"))
                    Text(tr(language, "The local catalog stays empty until real app details and URLs are provided.", "আসল অ্যাপের তথ্য ও URL না পাওয়া পর্যন্ত তালিকাটি খালি থাকবে।"))
                }
            }
        }
        items(apps, key = { it.id }) { app ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(listOf(app.category, app.version.orEmpty()).filter(String::isNotBlank).joinToString(" • "), color = MaterialTheme.colorScheme.primary)
                    Text(app.description)
                    val url = app.downloadUrl ?: app.websiteUrl
                    if (!url.isNullOrBlank()) {
                        Button(onClick = { openHttpsUrl(context, url) }) { Text(tr(language, "Open link", "লিংক খুলুন")) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperScreen(language: Language, sections: List<InfoSection>, modifier: Modifier) {
    val context = LocalContext.current
    var reportPreview by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(112.dp)) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(Icons.Default.Person, null, modifier = Modifier.size(56.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("𝙼𝚁  𝚂𝙷𝙰𝙳𝙾𝚆", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(tr(language, "Developer", "ডেভেলপার"))
        Spacer(Modifier.height(18.dp))
        ContactCard("WhatsApp", "https://wa.me/message/AXTGUAHKBLIHL1", Icons.Default.Chat) { openHttpsUrl(context, it) }
        Spacer(Modifier.height(8.dp))
        ContactCard("Telegram", "https://t.me/mr_shadow23", Icons.Default.Send) { openHttpsUrl(context, it) }
        Spacer(Modifier.height(18.dp))
        Button(onClick = { reportPreview = sections.asReport() }, modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.Icon(Icons.Default.Security, null)
            Text(tr(language, "Review & share report", "রিপোর্ট দেখে শেয়ার করুন"))
        }
        Text(
            tr(language, "Nothing is sent automatically. Confirming opens Android Sharesheet; no backend is configured.", "কিছু স্বয়ংক্রিয়ভাবে পাঠানো হয় না। নিশ্চিত করলে Android Sharesheet খুলবে; backend নেই।"),
            style = MaterialTheme.typography.bodySmall
        )
    }
    reportPreview?.let { report ->
        AlertDialog(
            onDismissRequest = { reportPreview = null },
            title = { Text(tr(language, "Review report", "রিপোর্ট যাচাই")) },
            text = {
                Column {
                    Text(tr(language, "Review the full report. Continue opens Android Sharesheet; this does not mean the report was delivered.", "সম্পূর্ণ রিপোর্ট দেখে নিন। এগোলে Android Sharesheet খুলবে; রিপোর্ট পৌঁছেছে এমন নয়।"))
                    Spacer(Modifier.height(8.dp))
                    Text(report, Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { reportPreview = null; shareText(context, report, "Share device report") }) {
                    Text(tr(language, "Continue", "এগিয়ে যান"))
                }
            },
            dismissButton = {
                TextButton(onClick = { reportPreview = null }) { Text(tr(language, "Cancel", "বাতিল")) }
            }
        )
    }
}

@Composable
private fun ContactCard(name: String, url: String, icon: ImageVector, onOpen: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onOpen(url) }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(name, Modifier.weight(1f))
            androidx.compose.material3.Icon(Icons.Default.OpenInNew, null)
        }
    }
}

@Composable
private fun SettingsScreen(
    language: Language,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onLanguageChange: (Language) -> Unit,
    showUnavailable: Boolean,
    onShowUnavailableChange: (Boolean) -> Unit,
    autoRefresh: Boolean,
    onAutoRefreshChange: (Boolean) -> Unit,
    notifications: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<String?>(null) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(tr(language, "Settings", "সেটিংস"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { ToggleRow(tr(language, "Appearance", "চেহারা"), tr(language, "Dark theme", "ডার্ক থিম"), darkTheme, onDarkThemeChange) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(tr(language, "Language", "ভাষা"), fontWeight = FontWeight.SemiBold)
                    Row {
                        TextButton(onClick = { onLanguageChange(Language.EN) }) { Text(if (language == Language.EN) "✓ English" else "English") }
                        TextButton(onClick = { onLanguageChange(Language.BN) }) { Text(if (language == Language.BN) "✓ বাংলা" else "বাংলা") }
                    }
                }
            }
        }
        item { ToggleRow(tr(language, "Device information", "ডিভাইস তথ্য"), tr(language, "Show unavailable fields", "অনুপলব্ধ তথ্য দেখান"), showUnavailable, onShowUnavailableChange) }
        item { ToggleRow(tr(language, "Device information", "ডিভাইস তথ্য"), tr(language, "Refresh on app launch", "অ্যাপ খোলার সময় রিফ্রেশ"), autoRefresh, onAutoRefreshChange) }
        item { ToggleRow(tr(language, "Notifications", "নোটিফিকেশন"), tr(language, "Enable notifications", "নোটিফিকেশন চালু করুন"), notifications, onNotificationsChange) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    TextButton(onClick = { dialog = "privacy" }) {
                        androidx.compose.material3.Icon(Icons.Default.Security, null)
                        Text(tr(language, "Privacy", "গোপনীয়তা"))
                    }
                    TextButton(onClick = { dialog = "about" }) {
                        androidx.compose.material3.Icon(Icons.Default.Info, null)
                        Text(tr(language, "About", "পরিচিতি"))
                    }
                    TextButton(onClick = { shareText(context, "MR SHADOW DEVICE INFO", "Support") }) {
                        androidx.compose.material3.Icon(Icons.Default.Share, null)
                        Text(tr(language, "Support / share app", "সহায়তা / অ্যাপ শেয়ার"))
                    }
                }
            }
        }
        item {
            Text(
                tr(language, "Notifications are a saved preference only; this build does not post notifications.", "নোটিফিকেশন কেবল সংরক্ষিত পছন্দ; এই সংস্করণ নোটিফিকেশন পাঠায় না।"),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    dialog?.let { kind ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(if (kind == "privacy") tr(language, "Privacy", "গোপনীয়তা") else tr(language, "About", "পরিচিতি")) },
            text = {
                Text(
                    if (kind == "privacy") tr(
                        language,
                        "No network permission or report backend is included. The app does not collect IMEI, serial number, phone/SIM data, location, accounts, credentials or personal files. Data stays on this device unless you explicitly share it through Android Sharesheet.",
                        "ইন্টারনেট permission বা report backend নেই। IMEI, serial, ফোন/SIM, অবস্থান, অ্যাকাউন্ট, credentials বা ব্যক্তিগত ফাইল সংগ্রহ হয় না। Android Sharesheet দিয়ে নিজে শেয়ার না করা পর্যন্ত তথ্য ডিভাইসেই থাকে।"
                    ) else "MR SHADOW DEVICE INFO\nDeveloper: 𝙼𝚁  𝚂𝙷𝙰𝙳𝙾𝚆\nVersion 1.0.0\nThis app does not root, unlock, flash firmware or install APKs."
                )
            },
            confirmButton = {
                TextButton(onClick = { dialog = null }) { Text(tr(language, "Close", "বন্ধ করুন")) }
            }
        )
    }
}

@Composable
private fun ToggleRow(group: String, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(group, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(title)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

private fun openHttpsUrl(context: Context, rawUrl: String) {
    val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return
    if (uri.scheme == "https" && !uri.host.isNullOrBlank()) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}

private fun shareText(context: Context, text: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    runCatching { context.startActivity(Intent.createChooser(intent, title)) }
}

private fun tr(language: Language, english: String, bengali: String): String =
    if (language == Language.BN) bengali else english
