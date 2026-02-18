package com.FactoryBrowser.browser

import android.content.Context
import android.os.Environment
import android.util.Log
import org.json.JSONObject
import java.io.File

data class UrlEntry(
    val label: String,
    val url: String,
    val icon: String
)

object AppConfig {

    private const val TAG = "AppConfig"
    private const val CONFIG_FOLDER   = "FactoryBrowser"
    private const val CONFIG_FILENAME = "factory_config.json"

    private var _urls: List<UrlEntry> = emptyList()
    private var _zoomRules: Map<String, Int> = emptyMap()
    private var _configSource: String = "unknown"
    private var _isConfigLoaded: Boolean = false

    val urls: List<UrlEntry>        get() = _urls
    val zoomRules: Map<String, Int> get() = _zoomRules
    val configSource: String        get() = _configSource
    val isConfigLoaded: Boolean     get() = _isConfigLoaded

    fun getDeviceConfigFile(): File {
        val dir = File(Environment.getExternalStorageDirectory(), CONFIG_FOLDER)
        return File(dir, CONFIG_FILENAME)
    }

    fun load(context: Context) {
        val deviceFile = getDeviceConfigFile()

        val (rawJson, source) = when {
            deviceFile.exists() && deviceFile.canRead() -> {
                Log.i(TAG, "Using device config: ${deviceFile.absolutePath}")
                Pair(deviceFile.readText(), "device")
            }
            else -> {
                Log.i(TAG, "Using bundled config from assets")
                try {
                    val bundled = context.assets.open(CONFIG_FILENAME)
                        .bufferedReader().use { it.readText() }
                    Pair(bundled, "bundled")
                } catch (e: Exception) {
                    Log.e(TAG, "No bundled config: ${e.message}")
                    _urls = emptyList()
                    _zoomRules = emptyMap()
                    _configSource = "NOT FOUND"
                    _isConfigLoaded = false
                    return
                }
            }
        }

        _configSource = source
        parse(rawJson)
    }

    private fun parse(json: String) {
        try {
            val obj = JSONObject(json)
            val list = mutableListOf<UrlEntry>()
            val arr = obj.optJSONArray("urls")
            if (arr != null) {
                val count = minOf(arr.length(), 4)
                for (i in 0 until count) {
                    val item = arr.getJSONObject(i)
                    list.add(UrlEntry(
                        label = item.optString("label", "App ${i + 1}"),
                        url   = item.optString("url", "https://example.com"),
                        icon  = item.optString("icon", "🌐")
                    ))
                }
            }
            _urls = list

            val zoom = mutableMapOf<String, Int>()
            obj.optJSONObject("zoomRules")?.keys()?.forEach { key ->
                if (!key.startsWith("_"))
                    zoom[key] = obj.getJSONObject("zoomRules").getInt(key)
            }
            _zoomRules = zoom
            _isConfigLoaded = true
            Log.i(TAG, "Loaded ${_urls.size} URLs from $_configSource")

        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
            _urls = emptyList()
            _zoomRules = emptyMap()
            _isConfigLoaded = false
        }
    }
}
