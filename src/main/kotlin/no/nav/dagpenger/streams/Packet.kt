package no.nav.dagpenger.streams

import org.json.JSONObject
import java.lang.IllegalArgumentException

class Packet internal constructor(jsonString: String) {

    val json: JSONObject = JSONObject(jsonString)

    init {
        if (json.has("system_read_count")) json.put("system_read_count", json.getInt("system_read_count") + 1)
        else json.put("system_read_count", 1)
    }

    fun getValue(key: String): Any? = json.get(key)

    fun put(key: String, value: Any) {
        if (json.has(key)) throw IllegalArgumentException("Cannot overwrite existing key: $key")
        json.put(key, value)
    }

    fun toJson(): String? = json.toString()

    fun hasField(key: String): Boolean = json.has(key)
}