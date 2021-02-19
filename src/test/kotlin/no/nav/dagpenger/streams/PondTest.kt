package no.nav.dagpenger.streams

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.Helpers.keySerializer
import no.nav.dagpenger.streams.Helpers.topicName
import no.nav.dagpenger.streams.Helpers.valueSerializer
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.Predicate
import org.apache.logging.log4j.ThreadContext
import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.test.assertEquals

class PondTest {

    companion object {

        val config = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }
    }

    class TestPond(val store: MutableList<Packet>) : Pond(Topics.DAGPENGER_BEHOV_PACKET_EVENT) {
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

            val testDriver = topologyTestDriver.createInputTopic(
                topicName,
                keySerializer,
                valueSerializer
            )

            testDriver.pipeInput(packet)
            assertEquals(1, testPond.store.size)
        }
    }

    @Test
    fun ` Should ignore package if filter does not match`() {
        val testPond = TestPond(mutableListOf())

        TopologyTestDriver(testPond.buildTopology(), config).use { topologyTestDriver ->
            val packet = Packet("{}").apply { this.putValue("Field", "Value") }

            val testDriver = topologyTestDriver.createInputTopic(
                topicName,
                keySerializer,
                valueSerializer
            )

            testDriver.pipeInput(packet)
            assertEquals(0, testPond.store.size)
        }
    }

    @Test
    fun `Should have correlation id `() {
        val service = object : Pond(Topics.DAGPENGER_BEHOV_PACKET_EVENT) {
            override val SERVICE_APP_ID: String = "correlation_id"

            override fun filterPredicates(): List<Predicate<String, Packet>> {
                return listOf(Predicate { _, packet -> !packet.hasField("new") })
            }

            override fun onPacket(packet: Packet) {
                ThreadContext.get("x_correlation_id") shouldNotBe null
            }
        }

        TopologyTestDriver(service.buildTopology(), RiverTest.config).use { topologyTestDriver ->
            val packet = Packet("{}")

            val testDriver = topologyTestDriver.createInputTopic(
                topicName,
                keySerializer,
                valueSerializer
            )

            testDriver.pipeInput(packet)
        }
        ThreadContext.get("x_correlation_id") shouldBe null
    }
}
