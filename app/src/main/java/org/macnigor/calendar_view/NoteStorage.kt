package org.macnigor.calendar_view

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

class NoteStorage(context: Context) {
    private val prefs = context.getSharedPreferences("note_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val type = object : TypeToken<Map<String, String>>() {}.type

    fun saveNotes(notes: Map<LocalDate, String>) {
        val stringMap = notes.mapKeys { it.key.toString() } // LocalDate -> String
        val json = gson.toJson(stringMap)
        prefs.edit().putString("notes", json).apply()
    }

    fun loadNotes(): Map<LocalDate, String> {
        val json = prefs.getString("notes", null) ?: return emptyMap()
        val stringMap: Map<String, String> = gson.fromJson(json, type)
        return stringMap.mapKeys { LocalDate.parse(it.key) }
    }
}
