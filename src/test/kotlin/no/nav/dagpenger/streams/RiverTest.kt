package no.nav.dagpenger.streams

import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.Problem
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.Predicate
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.apache.logging.log4j.ThreadContext
import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RiverTest {

    companion object {

        private val testTopic = Topic(
            name = "test-topic",
            keySerde = Serdes.String(),
            valueSerde = Serdes.serdeFrom(PacketSerializer(), PacketDeserializer()))

        val factoryForTestTopic = ConsumerRecordFactory<String, Packet>(
            testTopic.name,
            testTopic.keySerde.serializer(),
            testTopic.valueSerde.serializer()
        )
        val factoryForDagpengerBehovTopic = ConsumerRecordFactory<String, Packet>(
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.serializer(),
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.serializer()
        )

        val config = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }
    }

    class TestService : River(Topics.DAGPENGER_BEHOV_PACKET_EVENT) {
        override val SERVICE_APP_ID = "TestService"

        override fun filterPredicates(): List<Predicate<String, Packet>> {
            return listOf(Predicate { _, packet -> !packet.hasField("new") })
        }

        override fun onPacket(packet: Packet): Packet {
            packet.putValue("new", "newvalue")
            return packet
        }
    }

    class TestTopicService : River(testTopic) {
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
            val inputRecord = factoryForDagpengerBehovTopic.create(Packet(jsonString))
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

    class FailingTestService : River(Topics.DAGPENGER_BEHOV_PACKET_EVENT) {
        override val SERVICE_APP_ID = "TestService"

        override fun filterPredicates(): List<Predicate<String, Packet>> {
            return listOf(Predicate { _, packet -> !packet.hasField("new") })
        }

        override fun onPacket(packet: Packet): Packet {
            throw RuntimeException("Fail to process")
        }
    }

    class FailingTestServiceOnFailure : River(Topics.DAGPENGER_BEHOV_PACKET_EVENT) {
        override val SERVICE_APP_ID = "TestService"

        override fun filterPredicates(): List<Predicate<String, Packet>> {
            return listOf(Predicate { _, packet -> !packet.hasField("new") })
        }

        override fun onPacket(packet: Packet): Packet {
            throw RuntimeException("Fail to process")
        }

        override fun onFailure(packet: Packet, error: Throwable?): Packet {
            packet.addProblem(
                Problem(
                    title = error!!.message!!

                )
            )
            return packet
        }
    }

    @Test
    fun ` River should add problem if service fails to process onPacket `() {
        val testService = FailingTestService()

        TopologyTestDriver(testService.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factoryForDagpengerBehovTopic.create(Packet(jsonString))
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
            val inputRecord = factoryForDagpengerBehovTopic.create(Packet(jsonString))
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertTrue { ut != null }
            assertTrue { ut.value().hasProblem() }
            assertEquals("Fail to process", ut.value().getProblem()?.title)
        }
    }

    @Test
    fun ` Should be able to ovveride topic in River`() {
        val testService = TestTopicService()
        TopologyTestDriver(testService.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factoryForTestTopic.create(Packet(jsonString))
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                "test-topic",
                Serdes.String().deserializer(),
                PacketDeserializer()
            )

            assertTrue { ut != null }
            assertEquals("newvalue", ut.value().getNullableStringValue("new"))
            assertEquals(1, ut.value().getNullableIntValue("key1"))
            assertEquals("value1", ut.value().getNullableStringValue("key2"))
            assertEquals(true, ut.value().getBoolean("key3"))
        }
    }

    @Test
    fun `Should have correlation id `() {
        val service = object : River(testTopic) {
            override val SERVICE_APP_ID: String = "correlation_id"

            override fun filterPredicates(): List<Predicate<String, Packet>> {
                return listOf(Predicate { _, packet -> !packet.hasField("new") })
            }

            override fun onPacket(packet: Packet): Packet {
                packet.putValue("new", "value")
                ThreadContext.get("x_correlation_id") shouldNotBe null
                return packet
            }

            override fun onFailure(packet: Packet, error: Throwable?): Packet {
                throw AssertionError("No correlation id?", error)
            }
        }

        TopologyTestDriver(service.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factoryForTestTopic.create(Packet(jsonString))
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                "test-topic",
                Serdes.String().deserializer(),
                PacketDeserializer()
            )
            assertTrue { ut != null }
        }
    }

    private val jsonString = """
            {
                "key1": 1,
                "key2": "value1",
                "key3": true
            }
        """.trimIndent()
}
