package no.nav.dagpenger.streams

import com.google.gson.JsonSyntaxException
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import no.nav.dagpenger.events.avro.BruktInntektsperiode
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertTrue

class PacketTest {
    @Test
    fun `create packet with JSON`() {
        val jsonString = """
            {
                "key1": "value1"
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
        assertThrows<JsonSyntaxException> { Packet(invalidJson) }
    }

    @Test
    fun `packet keeps fields`() {
        val jsonString = """
            {
                "key1": 1,
                "key2": "value1",
                "key3": true
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
                "key1": "value1"
            }
        """.trimIndent()

        assertTrue(JSONObject(Packet(jsonString).toJson()).has("system_read_count"))
        assertEquals(0, JSONObject(Packet(jsonString).toJson()).getInt("system_read_count"))
    }

    @Test
    fun `packet increments system readcount`() {
        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1"
            }
        """.trimIndent()

        assertEquals(6, JSONObject(Packet(jsonString).toJson()).getInt("system_read_count"))
    }

    @Test
    fun `packet does not allow rewrite`() {

        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1"
            }
        """.trimIndent()

        assertThrows<IllegalArgumentException> { Packet(jsonString).put("key1", "awe") }
    }

    @Test
    fun `can write list to packet`() {

        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1"
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
                "key1": "value1"
            }
        """.trimIndent()
        val packet = Packet(jsonString).also { it.put("dec", BigDecimal(5)) }
        val packet2 = Packet(jsonString).also { it.put("dec", BigDecimal(5.3)) }
        assertEquals(BigDecimal(5), packet.getValue("dec"))
        assertEquals(BigDecimal(5.3), packet2.getValue("dec"))
    }

    @Test
    fun `hasField `() {

        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1",
                "anotherKey": "qwe"
            }
        """.trimIndent()
        val packet = Packet(jsonString)
        assertTrue(packet.hasField("key1"))
        assertTrue(packet.hasField("anotherKey"))
        assertFalse(packet.hasField("some other key"))
        assertFalse(packet.hasField("key2"))
    }

    data class ClassA(
        val inntektsId: String,
        val inntektsListe: List<ClassB>
    )

    data class ClassB(
        val årMåned: YearMonth,
        val klassifiserteInntekter: List<ClassC>
    )

    data class ClassC(
        val beløp: BigDecimal,
        val inntektKlasse: AnEnum
    )

    enum class AnEnum {
        ARBEIDSINNTEKT,
        DAGPENGER,
        DAGPENGER_FANGST_FISKE,
        SYKEPENGER_FANGST_FISKE,
        NÆRINGSINNTEKT,
        SYKEPENGER,
        TILTAKSLØNN
    }

    @Test
    fun `can put complex object`() {
        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1",
                "anotherKey": "qwe"
            }
        """.trimIndent()
        val packet = Packet(jsonString)
        val complex = ClassA(
            "innntektsId", listOf(
                ClassB(
                    YearMonth.of(2019, 2),
                    listOf(ClassC(BigDecimal.ZERO, AnEnum.TILTAKSLØNN), ClassC(BigDecimal.TEN, AnEnum.ARBEIDSINNTEKT))
                )
            )
        )
        packet.put("complex", complex)

        assertEquals(complex, packet.getValue("complex"))
        assertEquals(packet.toJson(), Packet(packet.toJson()!!).toJson())
    }
}