package com.mrshadow.deviceinfo.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import java.util.Locale

private const val UNAVAILABLE = "Not Available"

data class InfoField(val label: String, val value: String)
data class InfoSection(val title: String, val fields: List<InfoField>) {
    fun report(): String = fields.joinToString("\n") { "${it.label}: ${it.value}" }
}

class DeviceInfoService(private val context: Context) {
    fun collect(): List<InfoSection> {
        fun value(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: UNAVAILABLE

        val basic = listOf(
            InfoField("Manufacturer", value(Build.MANUFACTURER)),
            InfoField("Brand", value(Build.BRAND)),
            InfoField("Model", value(Build.MODEL)),
            InfoField("Device", value(Build.DEVICE)),
            InfoField("Product", value(Build.PRODUCT)),
            InfoField("Board", value(Build.BOARD)),
            InfoField("Hardware", value(Build.HARDWARE)),
            InfoField("Build number", value(Build.DISPLAY)),
            InfoField("Device type", if (context.resources.configuration.smallestScreenWidthDp >= 600) "Tablet" else "Phone")
        )

        val os = listOf(
            InfoField("Android version", value(Build.VERSION.RELEASE)),
            InfoField("Android SDK", Build.VERSION.SDK_INT.toString()),
            InfoField("Build ID", value(Build.ID)),
            InfoField("Security patch", value(Build.VERSION.SECURITY_PATCH)),
            InfoField("OS version", value(System.getProperty("os.version"))),
            InfoField("System architecture", value(System.getProperty("os.arch"))),
            InfoField("Bootloader version", value(Build.BOOTLOADER)),
            InfoField("Root status", UNAVAILABLE),
            InfoField("Bootloader unlock status", UNAVAILABLE)
        )

        val cpu = listOf(
            InfoField("Hardware / chipset", value(Build.HARDWARE)),
            InfoField("Supported CPU ABI", Build.SUPPORTED_ABIS.joinToString().ifBlank { UNAVAILABLE }),
            InfoField("Available processor cores", Runtime.getRuntime().availableProcessors().toString()),
            InfoField("Maximum CPU frequency", maxCpuFrequency())
        )

        val memory = runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            ActivityManager.MemoryInfo().also(am::getMemoryInfo)
        }.getOrNull()

        val ram = listOf(
            InfoField("Total RAM", memory?.totalMem?.let(::formatBytes) ?: UNAVAILABLE),
            InfoField("Available RAM", memory?.availMem?.let(::formatBytes) ?: UNAVAILABLE)
        )

        val storage = runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            listOf(
                InfoField("Internal storage total", formatBytes(stat.totalBytes)),
                InfoField("Internal storage available", formatBytes(stat.availableBytes)),
                InfoField("Internal storage used", formatBytes((stat.totalBytes - stat.availableBytes).coerceAtLeast(0)))
            )
        }.getOrElse {
            listOf(InfoField("Internal storage", UNAVAILABLE))
        }

        val batteryIntent = runCatching {
            context.registerReceiver(null, Intent(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1

        val battery = listOf(
            InfoField("Battery level", if (level >= 0 && scale > 0) "${level * 100 / scale}%" else UNAVAILABLE),
            InfoField("Charging status", when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
                else -> UNAVAILABLE
            }),
            InfoField("Battery health", when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> UNAVAILABLE
            }),
            InfoField("Temperature", batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)?.takeIf { it >= 0 }?.let {
                String.format(Locale.US, "%.1f °C", it / 10f)
            } ?: UNAVAILABLE),
            InfoField("Voltage", batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }?.let { "$it mV" } ?: UNAVAILABLE),
            InfoField("Technology", value(batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)))
        )

        val metrics = context.resources.displayMetrics
        val display = listOf(
            InfoField("Resolution", "${metrics.widthPixels} × ${metrics.heightPixels} px"),
            InfoField("Density", String.format(Locale.US, "%.2f", metrics.density)),
            InfoField("Screen DPI", "${metrics.densityDpi} dpi")
        )

        return listOf(
            InfoSection("Basic device information", basic),
            InfoSection("Android & operating system", os),
            InfoSection("Processor", cpu),
            InfoSection("Memory (RAM)", ram),
            InfoSection("Storage", storage),
            InfoSection("Battery", battery),
            InfoSection("Display", display),
            InfoSection("Camera", listOf(InfoField("Camera metadata", UNAVAILABLE)))
        )
    }

    private fun maxCpuFrequency(): String = runCatching {
        val khz = java.io.File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").readText().trim().toLong()
        if (khz > 0) "${khz / 1000} MHz" else UNAVAILABLE
    }.getOrDefault(UNAVAILABLE)

    private fun formatBytes(bytes: Long): String =
        if (bytes > 0) Formatter.formatFileSize(context, bytes) else UNAVAILABLE
}

fun List<InfoSection>.asReport(): String = joinToString("\n\n") { section ->
    "${section.title}\n" + section.fields.joinToString("\n") { "${it.label}: ${it.value}" }
}
