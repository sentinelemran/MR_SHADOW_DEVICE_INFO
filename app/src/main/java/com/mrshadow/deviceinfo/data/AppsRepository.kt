package com.mrshadow.deviceinfo.data

import android.content.Context
import org.json.JSONArray

class AppsRepository(private val context: Context) {
    data class AppItem(
        val id: String,
        val name: String,
        val description: String,
        val category: String,
        val downloadUrl: String? = null,
        val websiteUrl: String? = null,
        val version: String? = null
    )

    fun load(): List<AppItem> = runCatching {
        val json = context.assets.open("apps.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            AppItem(
                id = item.optString("id", index.toString()),
                name = item.optString("name"),
                description = item.optString("description"),
                category = item.optString("category"),
                downloadUrl = item.optString("downloadUrl").takeIf(String::isNotBlank),
                websiteUrl = item.optString("websiteUrl").takeIf(String::isNotBlank),
                version = item.optString("version").takeIf(String::isNotBlank)
            )
        }
    }.getOrDefault(emptyList())
}
