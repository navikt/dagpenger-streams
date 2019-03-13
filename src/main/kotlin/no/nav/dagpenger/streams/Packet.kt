package no.nav.dagpenger.streams

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.lang.IllegalArgumentException

class Packet internal constructor(jsonString: String) {

    companion object {
        internal const val READ_COUNT = "system_read_count"
    }

    private val gsonBuilder = GsonBuilder().serializeNulls()
    private val jsonEngine = gsonBuilder.create()

    val json = jsonEngine.fromJson<MutableMap<String, Any?>>(jsonString, HashMap::class.java)

    init {
        if (!json.containsKey(READ_COUNT)) {
            json[READ_COUNT] = -1.0
        }
        json[READ_COUNT] = (json[READ_COUNT] as Double).toInt() + 1
    }

    fun getValue(key: String): Any? = json[key]

    fun put(key: String, value: Any) {
        if (json.containsKey(key)) throw IllegalArgumentException("Cannot overwrite existing key: $key")
        json[key] = value
    }

    fun toJson(): String? = jsonEngine.toJson(json)

    fun hasField(key: String): Boolean = json.containsKey(key)
}