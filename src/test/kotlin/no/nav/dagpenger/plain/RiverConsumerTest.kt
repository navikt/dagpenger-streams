package no.nav.dagpenger.plain

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.function.Predicate
import kotlin.test.assertEquals

class RiverConsumerTest {
    companion object {
        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        val embeddedEnvironment = KafkaEnvironment(
            users = listOf(JAASCredential(username, password)),
            autoStart = false,
            withSchemaRegistry = false,
            withSecurity = false,
            topicInfos = listOf(KafkaEnvironment.TopicInfo(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name))
        )

        @BeforeAll
        @JvmStatic
        fun start() {
            embeddedEnvironment.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            embeddedEnvironment.tearDown()
        }
    }

    class TestService : RiverConsumer(embeddedEnvironment.brokersURL) {
        override fun filterPredicates(): List<Predicate<Packet>> {
            return listOf(Predicate { r -> !r.hasField("new") })
        }

        override fun onPacket(packet: Packet): Packet {
            packet.putValue("new", "newvalue")
            return packet
        }

        override fun getConsumerConfig(credential: KafkaCredential?): Properties {
            return super.getConsumerConfig(credential).apply { put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest") }
        }

        override val SERVICE_APP_ID: String = "TestService"
    }

    class FailingTestService : RiverConsumer(embeddedEnvironment.brokersURL) {
        override fun filterPredicates(): List<Predicate<Packet>> {
            return listOf(Predicate { packet -> !packet.hasField("new") })
        }

        override fun onPacket(packet: Packet): Packet {
            throw RuntimeException("Fail to process")
        }

        override fun getConsumerConfig(credential: KafkaCredential?): Properties {
            return super.getConsumerConfig(credential).apply { put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest") }
        }

        override val SERVICE_APP_ID: String = "FailingTestService"
    }

    class FailingTestServiceOnFailure : RiverConsumer(embeddedEnvironment.brokersURL) {
        override val SERVICE_APP_ID = "TestServiceOnFailure"

        override fun filterPredicates(): List<Predicate<Packet>> {
            return listOf(Predicate { packet -> !packet.hasField("new") })
        }

        override fun onPacket(packet: Packet): Packet {
            throw RuntimeException("Fail to process")
        }

        override fun getConsumerConfig(credential: KafkaCredential?): Properties {
            return super.getConsumerConfig(credential).apply { put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest") }
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

    class ShouldNotRunDueToProblemService : RiverConsumer(embeddedEnvironment.brokersURL) {
        override fun filterPredicates(): List<Predicate<Packet>> {
            return emptyList()
        }

        override fun onPacket(packet: Packet): Packet {
            if (packet.getProblem() != null) {
                fail<Packet>("on packet was run for a filtered message")
            }
            return packet
        }

        override fun getConsumerConfig(credential: KafkaCredential?): Properties {
            return super.getConsumerConfig(credential).apply { put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest") }
        }

        override val SERVICE_APP_ID: String = "ShouldNotRunOnPacket"
    }

    @Test
    fun `embedded kafka cluster is up and running `() {
        assertEquals(embeddedEnvironment.serverPark.status, KafkaEnvironment.ServerParkStatus.Started)
    }

    @Test
    fun `should not run for packets with problems`() {
        runBlocking {
            val testService = ShouldNotRunDueToProblemService()
            testService.start()
            val packetWithProblem = Packet(jsonString).apply {
                addProblem(Problem(detail = "ShouldNotBeHere", title = "Failing test"))
            }
            val producer =
                KafkaProducer<String, Packet>(producerConfig("testMessageProducer", embeddedEnvironment.brokersURL))
            producer.send(ProducerRecord(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name, "test", packetWithProblem))
                .get(5, TimeUnit.SECONDS)
            producer.flush()
            producer.close()
            delay(2000)
            testService.stop()
        }
    }

    @Test
    fun `should add field called new`() {
        runBlocking {
            val testService = TestService()
            testService.start()
            val producer =
                KafkaProducer<String, Packet>(producerConfig("testMessageProducer", embeddedEnvironment.brokersURL))
            producer.send(ProducerRecord(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name, "test", Packet(jsonString)))
                .get(5, TimeUnit.SECONDS)
            producer.flush()
            producer.close()
            delay(2000)
            KafkaConsumer<String, Packet>(
                consumerConfig(
                    "test-verifier",
                    embeddedEnvironment.brokersURL,
                    properties = defaultConsumerConfig.apply { put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest") }
                )
            ).use { consumer ->
                consumer.subscribe(listOf(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name))
                val records = consumer.poll(Duration.of(5, ChronoUnit.SECONDS))
                assertTrue(records.any { it.value().hasField("new") })
            }
            testService.stop()
        }
    }

    @Test
    fun `river should add problem if service fails to process onPacket`() {
        runBlocking {
            val testService = FailingTestService()
            testService.start()
            val producer =
                KafkaProducer<String, Packet>(producerConfig("testMessageProducer", embeddedEnvironment.brokersURL))
            producer.send(ProducerRecord(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name, "test", Packet(jsonString)))
                .get(5, TimeUnit.SECONDS)
            producer.flush()
            delay(2000)
            KafkaConsumer<String, Packet>(
                consumerConfig(
                    "test-verifier",
                    embeddedEnvironment.brokersURL,
                    properties = defaultConsumerConfig.apply { put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest") }
                )
            ).use { consumer ->
                consumer.subscribe(listOf(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name))
                val records = consumer.poll(Duration.of(5, ChronoUnit.SECONDS))
                assertTrue(records.any { it.value().hasProblem() })
            }
            testService.stop()
        }
    }

    @Test
    fun `River should add problem if service fails to process onPacket with specified onFailure problem`() {
        runBlocking {
            val testService = FailingTestServiceOnFailure()
            val producer =
                KafkaProducer<String, Packet>(producerConfig("testMessageProducer", embeddedEnvironment.brokersURL))
            testService.start()
            producer.send(ProducerRecord(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name, "test", Packet(jsonString)))
                .get(5, TimeUnit.SECONDS)
            producer.close()
            delay(2000)
            KafkaConsumer<String, Packet>(
                consumerConfig(
                    "test-verifier",
                    embeddedEnvironment.brokersURL,
                    properties = defaultConsumerConfig.apply { put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest") }
                )
            ).use { consumer ->
                consumer.subscribe(listOf(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name))
                val records = consumer.poll(Duration.of(5, ChronoUnit.SECONDS))
                assertTrue(records.any { it.value().getProblem()?.title == "Fail to process" })
            }
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