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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mrshadow.deviceinfo.data.AppsRepository
import com.mrshadow.deviceinfo.device.DeviceInfoService
import com.mrshadow.deviceinfo.device.InfoSection
import com.mrshadow.deviceinfo.device.asReport
import com.mrshadow.deviceinfo.network.DeviceReportRequest
import com.mrshadow.deviceinfo.network.ReportApi
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appsRepository: AppsRepository
    @Inject lateinit var reportApi: ReportApi?
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShadowApp(appsRepository, reportApi) }
    }
}

private enum class Page(val label: String) { Home("Home"), Apps("Apps"), Developer("Developer"), Settings("Settings") }

@Composable
private fun ShadowApp(appsRepository: AppsRepository, reportApi: ReportApi?) {
    var page by remember { mutableStateOf(Page.Home) }
    var fields by remember { mutableStateOf(emptyList<InfoSection>()) }
    var dark by remember { mutableStateOf(true) }
    var language by remember { mutableStateOf("English") }
    var showUnavailable by remember { mutableStateOf(true) }
    var autoRefresh by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val service = remember(context) { DeviceInfoService(context) }
    fun refresh() { fields = runCatching { service.collect() }.getOrDefault(emptyList()) }
    LaunchedEffect(Unit) { refresh() }
    val colors = if (dark) darkColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF9BEA45), background = androidx.compose.ui.graphics.Color(0xFF101110), surface = androidx.compose.ui.graphics.Color(0xFF1B1E1B)) else lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF397A12))
    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("MR SHADOW DEVICE INFO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }) },
            bottomBar = {
                NavigationBar {
                    Page.entries.forEach { item ->
                        val icon = when (item) { Page.Home -> Icons.Default.Home; Page.Apps -> Icons.Default.Apps; Page.Developer -> Icons.Default.Person; Page.Settings -> Icons.Default.Settings }
                        NavigationBarItem(selected = page == item, onClick = { page = item }, icon = { Icon(icon, contentDescription = item.label) }, label = { Text(item.label) })
                    }
                }
            }
        ) { padding ->
            when (page) {
                Page.Home -> HomeScreen(fields, showUnavailable, ::refresh, Modifier.padding(padding), context)
                Page.Apps -> AppsScreen(appsRepository.load(), Modifier.padding(padding), context)
                Page.Developer -> DeveloperScreen(fields, reportApi, { refresh(); fields }, Modifier.padding(padding))
                Page.Settings -> SettingsScreen(dark, { dark = it }, language, { language = it }, autoRefresh, { autoRefresh = it }, showUnavailable, { showUnavailable = it }, Modifier.padding(padding), context)
            }
        }
    }
}

@Composable
private fun HomeScreen(fields: List<InfoSection>, showUnavailable: Boolean, onRefresh: () -> Unit, modifier: Modifier, context: Context) {
    val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(fields.asReport()) } }
    }
    Column(modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Your device", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Information available through standard Android APIs.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRefresh) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Refresh") }
                    OutlinedButton(onClick = { clipboard?.setPrimaryClip(ClipData.newPlainText("Device information", fields.asReport())) }) { Icon(Icons.Default.ContentCopy, null); Text("Copy") }
                    OutlinedButton(onClick = { createFile.launch("device-info.txt") }) { Icon(Icons.Default.Download, null); Text("Export") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { shareText(context, fields.asReport()) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(8.dp)); Text("Share device information") }
            }
            if (fields.isEmpty()) item { Text("Device information is temporarily unavailable.") }
            items(fields) { section -> InfoSectionCard(section, showUnavailable) }
        }
    }
}

@Composable
private fun InfoSectionCard(section: InfoSection, showUnavailable: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val visibleFields = if (showUnavailable) section.fields else section.fields.filterNot { it.value == "Not Available" }
    Card(Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(section.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) "Collapse" else "Expand")
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                visibleFields.forEach { field ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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
private fun AppsScreen(apps: List<AppsRepository.AppItem>, modifier: Modifier, context: Context) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Developer apps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("App links are loaded from the local assets/apps.json configuration.") }
        if (apps.isEmpty()) item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { Icon(Icons.Default.Apps, null); Spacer(Modifier.height(8.dp)); Text("No apps have been published yet.", fontWeight = FontWeight.SemiBold); Text("The developer can add official app details and URLs to the configuration.") } } }
        items(apps, key = { it.id }) { app ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${app.category}${app.version?.let { " • $it" } ?: ""}", color = MaterialTheme.colorScheme.primary)
                    Text(app.description)
                    val target = app.downloadUrl?.takeIf { it.isNotBlank() } ?: app.websiteUrl?.takeIf { it.isNotBlank() }
                    if (target != null) Button(onClick = { openUrl(context, target) }) { Text(if (app.downloadUrl == null) "Open website" else "Open / Download") }
                }
            }
        }
    }
}

@Composable
private fun DeveloperScreen(fields: List<InfoSection>, reportApi: ReportApi?, reload: () -> List<InfoSection>, modifier: Modifier) {
    val context = LocalContext.current
    var preview by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(110.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, Modifier.size(64.dp)) } }
        Spacer(Modifier.height(14.dp))
        Text("𝙼𝚁  𝚂𝙷𝙰𝙳𝙾𝚆", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Android Device Services\nRoot Support\nDevice Information Utility", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        ContactRow("WhatsApp", "https://wa.me/message/AXTGUAHKBLIHL1", Modifier.fillMaxWidth()) { openUrl(context, it) }
        Spacer(Modifier.height(10.dp))
        ContactRow("Telegram", "https://t.me/mr_shadow23", Modifier.fillMaxWidth()) { openUrl(context, it) }
        Spacer(Modifier.height(22.dp))
        Button(onClick = { val report = reload().asReport(); preview = report }, enabled = !sending, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Security, null); Spacer(Modifier.width(8.dp)); Text("Share Your Device Info With Developer")
        }
        Text("Nothing is sent unless you review and confirm the report.", style = MaterialTheme.typography.bodySmall)
        feedback?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp)) }
    }
    preview?.let { report ->
        AlertDialog(
            onDismissRequest = { if (!sending) preview = null },
            title = { Text("Review device information") },
            text = { Column {
                Text("This report will be sent to the developer. It excludes device identifiers, SIM details, location, accounts, and personal files.")
                Spacer(Modifier.height(10.dp))
                Text(report, Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall)
            } },
            confirmButton = {
                TextButton(enabled = !sending, onClick = {
                    sending = true
                    scope.launch {
                        val result = runCatching {
                            val api = reportApi ?: error("Secure report service is not configured.")
                            val response = api.submit(DeviceReportRequest(report))
                            response.isSuccessful && response.body()?.success == true
                        }.getOrDefault(false)
                        feedback = if (result) "Report sent successfully." else "Could not send report. No success was confirmed. Please try again later."
                        sending = false
                        preview = null
                    }
                }) { Text(if (sending) "Sending…" else "Confirm & send") }
            },
            dismissButton = { TextButton(enabled = !sending, onClick = { preview = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ContactRow(title: String, url: String, modifier: Modifier, onClick: (String) -> Unit) {
    Card(modifier.clickable { onClick(url) }) { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (title == "WhatsApp") Icons.Default.Chat else Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp)); Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Icon(Icons.Default.OpenInNew, "Open $title")
    } }
}

@Composable
private fun SettingsScreen(dark: Boolean, setDark: (Boolean) -> Unit, language: String, setLanguage: (String) -> Unit, autoRefresh: Boolean, setAuto: (Boolean) -> Unit, showUnavailable: Boolean, setUnavailable: (Boolean) -> Unit, modifier: Modifier, context: Context) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { SettingCard("Appearance", "Dark mode", dark, setDark) }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Language", fontWeight = FontWeight.SemiBold); Row(verticalAlignment = Alignment.CenterVertically) { listOf("English", "Bengali").forEach { label -> Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = language == label, onClick = { setLanguage(label) }); Text(label) } } } } } }
        item { SettingCard("Device information", "Auto refresh on app launch", autoRefresh, setAuto) }
        item { SettingCard("Device information", "Show unavailable fields", showUnavailable, setUnavailable) }
        item { SettingCard("Device information", "Show advanced information", true, {}) }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text("Privacy & support", fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { androidx.appcompat.app.AlertDialog.Builder(context).setTitle("Privacy information").setMessage("Device details remain on your device unless you explicitly review and confirm sharing. No IMEI, serial number, location, personal files, credentials, or SIM identifiers are collected. A configured HTTPS backend is required to send a report.").setPositiveButton("OK", null).show() }) { Text("Privacy policy / data sharing") }
            TextButton(onClick = { shareText(context, "MR SHADOW DEVICE INFO") }) { Text("Share app") }
            Text("MR SHADOW DEVICE INFO • Version 1.0.0")
            Text("Open-source licenses are provided by included libraries.", style = MaterialTheme.typography.bodySmall)
        } } }
        item { Text("Notifications: not enabled. Rooting, unlocking and flashing are not performed by this app.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun SettingCard(group: String, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(group, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary); Text(title) }
        Switch(checked = checked, onCheckedChange = onChange)
    } }
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, "Share device information"))
}
private fun openUrl(context: Context, url: String) {
    runCatching { Uri.parse(url).takeIf { it.scheme == "https" && !it.host.isNullOrBlank() }?.let { context.startActivity(Intent(Intent.ACTION_VIEW, it)) } }
}
