package no.nav.dagpenger.streams

import no.nav.dagpenger.events.Packet
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.Predicate
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PondTest {

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

    class TestPond(val store: MutableList<Packet>) : Pond() {
        override val SERVICE_APP_ID = "TestPond"

        override fun filterPredicates(): List<Predicate<String, Packet>> {
            return listOf(Predicate { _, packet -> !packet.hasField("Field") })
        }

        override fun onPacket(packet: Packet) {
            store.add(packet)
        }
    }

    @Test
    fun ` Should process package if filter matches`() {
        val testPond = TestPond(mutableListOf())

        TopologyTestDriver(testPond.buildTopology(), config).use { topologyTestDriver ->
            val packet = Packet("{}")

            topologyTestDriver.pipeInput(factory.create(packet))
            val ut = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertTrue { ut == null }
            assertEquals(1, testPond.store.size)
        }
    }

    @Test
    fun ` Should ignore package if filter does not match`() {
        val testPond = TestPond(mutableListOf())

        TopologyTestDriver(testPond.buildTopology(), config).use { topologyTestDriver ->
            val packet = Packet("{}").apply { this.putValue("Field", "Value") }

            topologyTestDriver.pipeInput(factory.create(packet))
            val ut = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertTrue { ut == null }
            assertEquals(0, testPond.store.size)
        }
    }
}
