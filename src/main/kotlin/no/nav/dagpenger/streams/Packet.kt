package no.nav.dagpenger.streams

import com.google.gson.GsonBuilder
import java.math.BigDecimal

class Packet internal constructor(jsonString: String) {

    companion object {
        internal const val READ_COUNT = "system_read_count"
    }

    private val gsonBuilder = GsonBuilder().serializeNulls().setLenient()
    private val jsonEngine = gsonBuilder.create()

    val json = jsonEngine.fromJson<MutableMap<String, Any?>>(jsonString, HashMap::class.java)

    init {
        if (!json.containsKey(READ_COUNT)) {
            json[READ_COUNT] = -1.0
        }
        json[READ_COUNT] = (json[READ_COUNT] as Double).toInt() + 1
    }

    private fun getValue(key: String): Any? = json[key]

    fun putValue(key: String, value: BigDecimal) {
        put(key, value.toPlainString())
    }

    fun putValue(key: String, value: Number) {
        put(key, value)
    }

    fun putValue(key: String, value: String) {
        put(key, value)
    }

    private fun put(key: String, value: Any) {
        if (json.containsKey(key)) throw IllegalArgumentException("Cannot overwrite existing key: $key")
        json[key] = value
    }

    fun toJson(): String? = jsonEngine.toJson(json)

    fun hasField(key: String): Boolean = json.containsKey(key)

    fun getBigDecimalValue(key: String): BigDecimal? {
        return getValue(key)?.let { BigDecimal(it.toString()) }
    }

    fun getIntValue(key: String): Int? {
        return getValue(key)?.toString()?.toInt()
    }

    fun getLongValue(key: String): Long? {
        return getValue(key)?.toString()?.toLong()
    }

    fun getStringValue(key: String): String? {
        return getValue(key)?.toString()
    }

    fun putValue(key: String, boolean: Boolean) {
        put(key, boolean)
    }

    fun getBoolean(key: String): Boolean? {
        val v: Any? = getValue(key)
        return when (v.toString().toLowerCase()) {
            "null" -> null
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("Value $v cannot be parsed to an Boolean")
        }
    }
}