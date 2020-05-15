package no.nav.dagpenger.streams

import kotlin.test.assertEquals
import kotlin.test.assertNull
import no.nav.dagpenger.events.Packet
import org.apache.kafka.common.errors.SerializationException
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PacketSerdeTest {
    @Test
    fun `deserializes to a packet`() {
        assertEquals(
            "value1",
            PacketDeserializer().deserialize("topic", jsonString.toByteArray())?.getNullableStringValue("key1")
        )
    }

    @Test
    fun `deserializes to null`() {
        assertNull(PacketDeserializer().deserialize("topic", null))
    }

    @Test
    fun `deserializing invalid json throws DeserializationException`() {
        assertThrows<SerializationException> { PacketDeserializer().deserialize("topic", "invalid json".toByteArray()) }
    }

    @Test
    fun `serializes from a packet`() {
        assertEquals(
            "value1",
            JSONObject(String(PacketSerializer().serialize("topic", Packet(jsonString))!!)).get("key1")
        )
    }

    @Test
    fun `serializes to null given null input`() {
        assertNull(PacketSerializer().serialize("topic", null))
    }

    val jsonString by lazy {
        Packet().apply { putValue("key1", "value1") }.toJson()!!
    }
}
