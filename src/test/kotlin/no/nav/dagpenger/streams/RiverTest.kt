package no.nav.dagpenger.streams

import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.KStream
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

        override fun river(stream: KStream<String, Packet>): KStream<String, Packet> {
            return stream
                .filter { _, packet -> !packet.hasField("new") }
                .mapValues { packet -> packet.also { it.put("new", "newvalue") } }
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
            assertEquals("newvalue", ut.value().getValue("new"))
            assertEquals(1, ut.value().getValue("key1"))
            assertEquals("value1", ut.value().getValue("key2"))
            assertEquals(true, ut.value().getValue("key3"))
        }
    }

    val jsonString = """
            {
                "key1": 1,
                "key2": "value1",
                "key3": true,
            }
        """.trimIndent()
}
