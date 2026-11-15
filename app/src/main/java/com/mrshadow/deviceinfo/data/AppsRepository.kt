package com.mrshadow.deviceinfo.data

import android.content.Context
import org.json.JSONArray

class AppsRepository(private val context: Context) {
    data class AppItem(
        val id: String,
        val name: String,
        val description: String,
        val category: String,
        val downloadUrl: String?,
        val websiteUrl: String?,
        val version: String?
    )

    fun load(): List<AppItem> = runCatching {
        val catalog = JSONArray(context.assets.open("apps.json").bufferedReader().use { it.readText() })
        (0 until catalog.length()).mapNotNull { index ->
            val entry = catalog.optJSONObject(index) ?: return@mapNotNull null
            AppItem(
                id = entry.optString("id", index.toString()),
                name = entry.optString("name"),
                description = entry.optString("description"),
                category = entry.optString("category"),
                downloadUrl = entry.optString("downloadUrl").takeIf(String::isNotBlank),
                websiteUrl = entry.optString("websiteUrl").takeIf(String::isNotBlank),
                version = entry.optString("version").takeIf(String::isNotBlank)
            )
        }
    }.getOrDefault(emptyList())
}
