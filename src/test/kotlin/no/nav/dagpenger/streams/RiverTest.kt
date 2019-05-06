package no.nav.dagpenger.streams

import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.Problem
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.Predicate
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.Test
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RiverTest {

    companion object {

        val factory = ConsumerRecordFactory<String, Packet>(
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.serializer(),
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.serializer()
        )

        val config = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }
    }

    class TestService : River() {
        override val SERVICE_APP_ID = "TestService"

        override fun filterPredicates(): List<Predicate<String, Packet>> {
            return listOf(Predicate { _, packet -> !packet.hasField("new") })
        }

        override fun onPacket(packet: Packet): Packet {
            packet.putValue("new", "newvalue")
            return packet
        }
    }

    @Test
    fun ` Should add field called new`() {
        val testService = TestService()

        TopologyTestDriver(testService.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(Packet(jsonString))
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertTrue { ut != null }
            assertEquals("newvalue", ut.value().getNullableStringValue("new"))
            assertEquals(1, ut.value().getNullableIntValue("key1"))
            assertEquals("value1", ut.value().getNullableStringValue("key2"))
            assertEquals(true, ut.value().getBoolean("key3"))
        }
    }

    class FailingTestService : River() {
        override val SERVICE_APP_ID = "TestService"

        override fun filterPredicates(): List<Predicate<String, Packet>> {
            return listOf(Predicate { _, packet -> !packet.hasField("new") })
        }

        override fun onPacket(packet: Packet): Packet {
            throw RuntimeException("Fail to process")
        }
    }

    class FailingTestServiceOnFailure : River() {
        override val SERVICE_APP_ID = "TestService"

        override fun filterPredicates(): List<Predicate<String, Packet>> {
            return listOf(Predicate { _, packet -> !packet.hasField("new") })
        }

        override fun onPacket(packet: Packet): Packet {
            throw RuntimeException("Fail to process")
            //   packet.putValue("new", "newvalue")
            // return packet
        }

        override fun onFailure(packet: Packet): Packet {
            packet.addProblem(
                Problem(
                    title = "ERROR"

                )
            )
            return packet
        }
    }

    @Test
    fun ` River should add problem if service fails to process onPacket `() {
        val testService = FailingTestService()

        TopologyTestDriver(testService.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(Packet(jsonString))
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertTrue { ut != null }
            assertTrue { ut.value().hasProblem() }
            assertEquals("Ukjent feil ved behandling av Packet", ut.value().getProblem()?.title)
        }
    }

    @Test
    fun ` River should add problem if service fails to process onPacket with specified onFailure proble`() {
        val testService = FailingTestServiceOnFailure()

        TopologyTestDriver(testService.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(Packet(jsonString))
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertTrue { ut != null }
            assertTrue { ut.value().hasProblem() }
            assertEquals("ERROR", ut.value().getProblem()?.title)
        }
    }

    val jsonString = """
            {
                "key1": 1,
                "key2": "value1",
                "key3": true
            }
        """.trimIndent()
}
