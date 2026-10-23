package com.rotacerta.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class HistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<HistoryEntry>> = _items.asStateFlow()

    @Synchronized
    fun add(e: HistoryEntry) {
        val list = (listOf(e) + _items.value).take(300)
        _items.value = list
        save(list)
    }

    @Synchronized
    fun clear() {
        _items.value = emptyList()
        save(emptyList())
    }

    private fun load(): List<HistoryEntry> = try {
        val arr = JSONArray(prefs.getString("items", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            HistoryEntry(
                o.getLong("t"), o.getDouble("p"), o.getDouble("km"), o.getInt("min"),
                o.getDouble("pkm"), o.getDouble("ph"), o.getDouble("pr"),
                Verdict.valueOf(o.getString("v")), o.optString("s")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun save(list: List<HistoryEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("t", e.timestamp); put("p", e.price); put("km", e.totalKm); put("min", e.totalMin)
                put("pkm", e.perKm); put("ph", e.perHour); put("pr", e.profit)
                put("v", e.verdict.name); put("s", e.source)
            })
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }
}
