package no.nav.dagpenger.streams

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.Test
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PacketServiceTest {

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

    class TestService : Service() {
        override val SERVICE_APP_ID = "TestService"

        override fun setupStreams(): KafkaStreams {
            return KafkaStreams(buildTopology(), config)
        }

        fun buildTopology(): Topology {
            val builder = StreamsBuilder()
            val stream = builder.stream(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Consumed.with(
                    Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde,
                    Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde
                )
            )
            stream
                .filter { _, packet -> !packet.hasField("new") }
                .mapValues { packet -> packet.also { it.writeField("new", "newvalue") } }
                .to(
                    Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                    Produced.with(
                        Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde,
                        Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde
                    )
                )

            return builder.build()
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
            assertEquals("newvalue", ut.value().getField("new"))
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

