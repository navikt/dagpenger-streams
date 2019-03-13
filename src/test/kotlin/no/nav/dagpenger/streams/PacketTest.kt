package no.nav.dagpenger.streams

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import kotlin.test.assertTrue

class PacketTest {
    @Test
    fun `create packet with JSON`() {
        val jsonString = """
            {
                "key1": "value1",
            }
        """.trimIndent()

        assertEquals("value1", Packet(jsonString).getValue("key1"))
    }

    @Test
    fun ` packet throws exception on invalid JSON`() {
        val invalidJson = """
            {
                "key1": "value1"
                "key2": "value1",
            }
        """.trimIndent()
        assertThrows<JSONException> { Packet(invalidJson) }
    }

    @Test
    fun `packet keeps fields`() {
        val jsonString = """
            {
                "key1": 1,
                "key2": "value1",
                "key3": true,
            }
        """.trimIndent()
        val jsonObject = JSONObject(Packet(jsonString).toJson())
        assertEquals(1, jsonObject.getInt("key1"))
        assertEquals("value1", jsonObject.getString("key2"))
        assertEquals(true, jsonObject.getBoolean("key3"))
    }

    @Test
    fun `packet gets system readcount`() {
        val jsonString = """
            {
                "key1": "value1",
            }
        """.trimIndent()

        assertTrue(JSONObject(Packet(jsonString).toJson()).has("system_read_count"))
        assertEquals(1, JSONObject(Packet(jsonString).toJson()).getInt("system_read_count"))
    }

    @Test
    fun `packet increments system readcount`() {
        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1",
            }
        """.trimIndent()

        assertEquals(6, JSONObject(Packet(jsonString).toJson()).getInt("system_read_count"))
    }

    @Test
    fun `packet does not allow rewrite`() {

        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1",
            }
        """.trimIndent()

        assertThrows<IllegalArgumentException> { Packet(jsonString).put("key1", "awe") }
    }

    @Test
    fun `can write list to packet`() {

        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1",
            }
        """.trimIndent()
        val packet = Packet(jsonString).also { it.put("list", listOf("awe", "qweqwe")) }
        assertEquals(listOf("awe", "qweqwe"), packet.getValue("list"))
    }

    @Test
    fun `can write BigDecimal to packet`() {

        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1",
            }
        """.trimIndent()
        val packet = Packet(jsonString).also { it.put("list", BigDecimal(5)) }
        assertEquals(BigDecimal(5), packet.getValue("list"))
    }

    @Test
    fun `hasField `() {

        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1",
                "anotherKey": "qwe",
            }
        """.trimIndent()
        val packet = Packet(jsonString)
        assertTrue(packet.hasField("key1"))
        assertTrue(packet.hasField("anotherKey"))
        assertFalse(packet.hasField("some other key"))
        assertFalse(packet.hasField("key2"))
    }
}