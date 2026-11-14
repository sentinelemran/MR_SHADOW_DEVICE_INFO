package com.mrshadow.deviceinfo.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.Locale
import kotlin.math.roundToInt

 data class InfoField(val label: String, val value: String)
data class InfoSection(val title: String, val fields: List<InfoField>) {
    fun report(): String = fields.joinToString("\n") { "${it.label}: ${it.value}" }
}

class DeviceInfoService(private val context: Context) {
    fun collect(): List<InfoSection> {
        val unavailable = "Not Available"
        fun v(value: String?): String = value?.takeIf { it.isNotBlank() } ?: unavailable
        val basic = listOf(
            InfoField("Manufacturer", v(Build.MANUFACTURER)), InfoField("Brand", v(Build.BRAND)),
            InfoField("Device model", v(Build.MODEL)), InfoField("Device name", v(Build.DEVICE)),
            InfoField("Codename", v(Build.DEVICE)), InfoField("Product", v(Build.PRODUCT)),
            InfoField("Board", v(Build.BOARD)), InfoField("Hardware manufacturer", v(Build.MANUFACTURER)),
            InfoField("Hardware model", v(Build.MODEL)), InfoField("Hardware revision", v(Build.HARDWARE)),
            InfoField("Build number", v(Build.DISPLAY)), InfoField("Device type", if (context.resources.configuration.smallestScreenWidthDp >= 600) "Tablet" else "Phone")
        )
        val os = listOf(
            InfoField("Android version", v(Build.VERSION.RELEASE)), InfoField("Android SDK", Build.VERSION.SDK_INT.toString()),
            InfoField("OS name", "Android"), InfoField("Custom ROM", unavailable), InfoField("OS version", v(System.getProperty("os.version"))),
            InfoField("Build ID", v(Build.ID)), InfoField("Build fingerprint", v(Build.FINGERPRINT)),
            InfoField("Security patch", v(if (Build.VERSION.SDK_INT >= 23) Build.VERSION.SECURITY_PATCH else null)),
            InfoField("Kernel version", kernel()), InfoField("Bootloader version", v(Build.BOOTLOADER)),
            InfoField("Baseband version", v(Build.getRadioVersion())), InfoField("Hardware version", v(Build.HARDWARE)),
            InfoField("System architecture", v(System.getProperty("os.arch"))), InfoField("CPU ABI", Build.SUPPORTED_ABIS.joinToString().ifBlank { unavailable }),
            InfoField("Root status", "Not reliably detectable"), InfoField("Bootloader status", "Not reliably detectable")
        )
        val cores = Runtime.getRuntime().availableProcessors()
        val cpu = listOf(
            InfoField("CPU manufacturer", unavailable), InfoField("CPU model / chipset", v(Build.HARDWARE)),
            InfoField("CPU architecture", v(System.getProperty("os.arch"))), InfoField("CPU ABI", Build.SUPPORTED_ABIS.joinToString()),
            InfoField("CPU cores", cores.toString()), InfoField("Maximum CPU frequency", maxCpuFrequency()), InfoField("GPU", unavailable)
        )
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val totalRam = if (Build.VERSION.SDK_INT >= 16) mem.totalMem else 0L
        val availableRam = mem.availMem
        val ram = listOf(
            InfoField("Physical RAM total", bytes(totalRam)), InfoField("Available memory", bytes(availableRam)),
            InfoField("Used physical RAM", if (totalRam > 0) bytes((totalRam - availableRam).coerceAtLeast(0)) else unavailable),
            InfoField("Memory classification", ramClass(totalRam))
        )
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val storage = listOf(
            InfoField("Internal storage total", bytes(total)), InfoField("Internal storage used", bytes((total - free).coerceAtLeast(0))),
            InfoField("Internal storage available", bytes(free)), InfoField("External storage", if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) "Available" else unavailable),
            InfoField("Storage type", unavailable)
        )
        val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryStatus = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val status = when (batteryStatus) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> unavailable
        }
        val temp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val battery = listOf(
            InfoField("Battery level", if (level >= 0 && scale > 0) "${level * 100 / scale}%" else unavailable),
            InfoField("Charging status", status), InfoField("Temperature", if (temp >= 0) String.format(Locale.US, "%.1f °C", temp / 10f) else unavailable),
            InfoField("Voltage", if (voltage > 0) "$voltage mV" else unavailable),
            InfoField("Technology", v(batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY))),
            InfoField("Health", health(batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1)),
            InfoField("Design capacity", unavailable)
        )
        val dm = context.resources.displayMetrics
        val resolution = "${dm.widthPixels} × ${dm.heightPixels} px"
        val refresh = runCatching {
            if (Build.VERSION.SDK_INT >= 30) context.display?.refreshRate else
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.refreshRate
        }.getOrNull()
        val display = listOf(
            InfoField("Resolution", resolution), InfoField("Screen width", "${dm.widthPixels} px"),
            InfoField("Screen height", "${dm.heightPixels} px"), InfoField("Density", String.format(Locale.US, "%.2f", dm.density)),
            InfoField("Screen DPI", "${dm.densityDpi} dpi"), InfoField("Display size", "Not Available (diagonal estimate not provided)"),
            InfoField("Refresh rate", refresh?.let { String.format(Locale.US, "%.0f Hz", it) } ?: unavailable), InfoField("Display technology", unavailable)
        )
        val camera = cameraFields()
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = if (Build.VERSION.SDK_INT >= 23) connectivity.activeNetwork?.let { connectivity.getNetworkCapabilities(it) } else null
        val networkType = when {
            network?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
            network?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
            network?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Disconnected / Not Available"
        }
        return listOf(
            InfoSection("Basic device information", basic), InfoSection("Android & operating system", os),
            InfoSection("Processor", cpu), InfoSection("Memory (physical RAM)", ram), InfoSection("Storage", storage),
            InfoSection("Battery", battery), InfoSection("Display", display), InfoSection("Camera", camera),
            InfoSection("Network & radio", listOf(InfoField("Network type", networkType), InfoField("Wi-Fi status", if (network?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true) "Connected" else "Not connected"), InfoField("SIM state", "Not collected"), InfoField("Radio information", v(Build.getRadioVersion()))))
        )
    }

    private fun cameraFields(): List<InfoField> = runCatching {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val chars = manager.cameraIdList.mapNotNull { id -> runCatching { id to manager.getCameraCharacteristics(id) }.getOrNull() }
        val rear = chars.filter { it.second.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK }
        val front = chars.filter { it.second.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT }
        val sizes = chars.flatMap { (_, c) -> c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.getOutputSizes(android.graphics.SurfaceTexture::class.java)?.toList().orEmpty() }
        val maxSize = sizes.maxByOrNull { it.width.toLong() * it.height }
        val modes = chars.flatMap { it.second.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.toList().orEmpty() }.distinct()
        listOf(
            InfoField("Camera sensors", chars.size.toString()), InfoField("Front cameras", front.size.toString()), InfoField("Rear cameras", rear.size.toString()),
            InfoField("Largest reported output", maxSize?.let { "${it.width} × ${it.height} px" } ?: "Not Available"),
            InfoField("Supported focus modes", if (modes.isEmpty()) "Not Available" else modes.joinToString()),
            InfoField("Flash", if (chars.any { it.second.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }) "Available" else "Not Available"),
            InfoField("Hardware level", chars.mapNotNull { it.second.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)?.toString() }.distinct().joinToString().ifBlank { "Not Available" })
        )
    }.getOrElse { listOf(InfoField("Camera details", "Not Available")) }

    private fun kernel(): String = runCatching { BufferedReader(FileReader("/proc/version")).use { it.readLine().orEmpty().substringBefore(" (" ) } }.getOrDefault("Not Available")
    private fun maxCpuFrequency(): String = runCatching {
        val khz = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").readText().trim().toLong()
        "${khz / 1000} MHz"
    }.getOrDefault("Not Available")
    private fun bytes(value: Long): String = if (value <= 0) "Not Available" else android.text.format.Formatter.formatFileSize(context, value)
    private fun ramClass(bytes: Long): String = if (bytes <= 0) "Not Available" else when {
        bytes < 3L * 1024 * 1024 * 1024 -> "Low (physical RAM)"
        bytes < 6L * 1024 * 1024 * 1024 -> "Standard (physical RAM)"
        else -> "High (physical RAM)"
    }
    private fun health(code: Int): String = when (code) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified failure"
        else -> "Not Available"
    }
}

fun List<InfoSection>.asReport(): String = joinToString("\n\n") { "${it.title}\n${it.report()}" }
