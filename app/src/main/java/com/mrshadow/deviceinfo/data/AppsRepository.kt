package com.mrshadow.deviceinfo

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    data class AppItem(
        val id: String,
        val name: String,
        val description: String,
        val category: String,
        val downloadUrl: String? = null,
        val websiteUrl: String? = null,
        val version: String? = null,
        val visible: Boolean = true
    )

    fun load(): List<AppItem> = runCatching {
        val json = context.assets.open("apps.json").bufferedReader().use { it.readText() }
        Gson().fromJson<List<AppItem>>(json, object : TypeToken<List<AppItem>>() {}.type) ?: emptyList()
    }.getOrDefault(emptyList()).filter { it.visible }
}
