package no.nav.dagpenger.streams

import com.squareup.moshi.JsonEncodingException
import org.json.JSONObject

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import kotlin.test.assertTrue

class PacketTest {
    @Test
    fun `create packet with JSON`() {
        val jsonString = """
            {
                "key1": "value1"
            }
        """.trimIndent()

        assertEquals("value1", Packet(jsonString).getStringValue("key1"))
    }

    @Test
    fun ` packet throws exception on invalid JSON`() {
        val invalidJson = """
            {
                "key1" "value1"
                "key2": "value1",
            }
        """.trimIndent()
        assertThrows<JsonEncodingException> { Packet(invalidJson) }
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
        assertTrue(JSONObject(Packet(jsonString).toJson()).has("system_started"))
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

        assertThrows<IllegalArgumentException> { Packet(jsonString).putValue("key1", "awe") }
    }

    @Test
    fun `can write BigDecimal to packet`() {

        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1"
            }
        """.trimIndent()
        val packet = Packet(jsonString)

        packet.putValue("dec", BigDecimal(5))
        packet.putValue("dec2", BigDecimal(5.3))
        packet.putValue("rubbish", "rubbish")

        assertEquals(BigDecimal(5), packet.getBigDecimalValue("dec"))
        assertEquals(BigDecimal(5.3), packet.getBigDecimalValue("dec2"))
        assertEquals(null, packet.getBigDecimalValue("notExisting"))
        assertThrows<NumberFormatException> { packet.getLongValue("rubbish") }
    }

    @Test
    fun `can write and get Number to packet`() {

        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1",
                "key2": 5
            }
        """.trimIndent()
        val packet = Packet(jsonString)

        packet.putValue("int", 1)
        packet.putValue("long", 1L)
        packet.putValue("rubbish", "rubbish")

        assertEquals(5, packet.getIntValue("key2"))
        assertEquals(1, packet.getIntValue("int"))
        assertEquals(1L, packet.getLongValue("long"))
        assertEquals(null, packet.getLongValue("noExisting"))
        assertThrows<NumberFormatException> { packet.getLongValue("rubbish") }
    }

    @Test
    fun `can write and get Boolean to packet`() {

        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1"
            }
        """.trimIndent()
        val packet = Packet(jsonString)

        packet.putValue("booleanValue1", true)
        packet.putValue("booleanValue2", false)
        packet.putValue("notAnBoolean", "rubbish")

        assertEquals(true, packet.getBoolean("booleanValue1"))
        assertEquals(false, packet.getBoolean("booleanValue2"))
        assertEquals(null, packet.getBoolean("notExistiing"))
        assertThrows<IllegalArgumentException> { packet.getBoolean("notAnBoolean") }
    }

    @Test
    fun `can write and get LocalDate to packet`() {

        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1"
            }
        """.trimIndent()
        val packet = Packet(jsonString)

        packet.putValue("localdate", LocalDate.of(2019, 1, 15))
        packet.putValue("notALocalDate", "rubbish")

        assertEquals(LocalDate.of(2019, 1, 15), packet.getLocalDate("localdate"))
        assertEquals(null, packet.getLocalDate("notExistiing"))
        assertThrows<DateTimeParseException> { packet.getLocalDate("notALocalDate") }
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

    @Test
    fun `hasFields `() {

        val jsonString = """
            {
                "system_read_count": 5,
                "key1": "value1",
                "anotherKey": "qwe",
                "thirdKey": "qwe"
            }
        """.trimIndent()
        val packet = Packet(jsonString)
        assertTrue(packet.hasFields("key1"))
        assertTrue(packet.hasFields("anotherKey"))
        assertFalse(packet.hasFields("some other key"))
        assertTrue(packet.hasFields("thirdKey", "key1", "anotherKey"))
        assertFalse(packet.hasFields("thirdKey", "key1", "non existing", "anotherKey"))
    }

    @Test
    fun `can put complex object`() {
        val jsonString = """
            {
                "system_read_count": 0,
                "key1": "value1",
                "anotherKey": "qwe"
            }
        """.trimIndent()
        val packet = Packet(jsonString)
        val complex = ClassA(
            "inntektsId", listOf(
                ClassB(
                    YearMonth.of(2019, 2),
                    listOf(ClassC(BigDecimal.ZERO, AnEnum.TILTAKSLØNN), ClassC(BigDecimal.TEN, AnEnum.ARBEIDSINNTEKT))
                )
            )
        )
        val adapter = moshiInstance.adapter<ClassA>(ClassA::class.java)

        packet.putValue("complex", complex, adapter::toJson)

        assertEquals(complex, packet.getObjectValue("complex", adapter::fromJson))

        val snapshot = Packet(packet.toJson()!!)
        assertEquals(complex, snapshot.getObjectValue("complex", adapter::fromJson))
        assertEquals("qwe", snapshot.getStringValue("anotherKey"))
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
}