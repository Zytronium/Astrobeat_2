package dev.zytronium.astrobeat2.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTagList(tags: List<String>?): String? {
        return tags?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toTagList(json: String?): List<String>? {
        if (json == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }
}
