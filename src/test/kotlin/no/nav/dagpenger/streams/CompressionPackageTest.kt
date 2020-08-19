package no.nav.dagpenger.streams

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import java.time.Duration
import java.util.Properties
import java.util.concurrent.TimeUnit
import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.moshiInstance
import no.nav.dagpenger.plain.consumerConfig
import no.nav.dagpenger.plain.producerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.Predicate
import org.junit.jupiter.api.Test
import org.testcontainers.containers.KafkaContainer

private val logger = KotlinLogging.logger { }

private val testTopic = Topic(
    "privat-dagpenger-test",
    keySerde = Serdes.String(),
    valueSerde = Serdes.serdeFrom(PacketSerializer(), PacketDeserializer())
)

private object Kafka {
    val instance by lazy {
        KafkaContainer("5.3.0").apply { this.start() }
    }
}

private data class TestData(val data: String, val number: Int, val double: Double, val moreData: String) {
    private val jsonAdapter = moshiInstance.adapter(TestData::class.java)
    fun toJson(): Any? = jsonAdapter.toJsonValue(this)

    companion object {
        fun generate(): Sequence<TestData> = generateSequence {
            TestData(
                data = Arb.string().next(),
                number = Arb.int().next(),
                double = Arb.double().next(),
                moreData = Arb.string().next()
            )
        }
    }
}

class CompressionPackageTest {

    class TestServiceThatAddBigData : River(Topics.DAGPENGER_BEHOV_PACKET_EVENT) {
        override val SERVICE_APP_ID = "TestService"
        override val withHealthChecks = false

        override fun filterPredicates(): List<Predicate<String, Packet>> {
            return listOf(Predicate { _, packet -> !packet.hasField("big-json") })
        }

        override fun onPacket(packet: Packet): Packet {
            val bigData = TestData.generate().take(7000).toList().map { it.toJson() }
            packet.putValue("big-json", bigData)
            return packet
        }

        override fun getConfig(): Properties {
            return streamConfig(SERVICE_APP_ID, Kafka.instance.bootstrapServers)
        }
    }

    class TestServiceThatAddBigPregeneratedData : River(testTopic) {
        override val SERVICE_APP_ID = "TestService2"
        override val withHealthChecks = false

        override fun filterPredicates(): List<Predicate<String, Packet>> {
            return listOf(Predicate { _, packet -> !packet.hasField("big-json") })
        }

        override fun onPacket(packet: Packet): Packet {
            val largeJson = this::class.java.getResource("/unormalt-stor-json.json").readText()
            packet.putValue(
                "big-json", largeJson

            )
            return packet
        }

        override fun getConfig(): Properties {
            return streamConfig(SERVICE_APP_ID, Kafka.instance.bootstrapServers)
        }
    }

    @Test
    fun `Should compress packages with randomly generated json`() {
        val producer = KafkaProducer<String, Packet>(
            producerConfig(
                clientId = "test",
                bootstrapServers = Kafka.instance.bootstrapServers
            ).also {
                it[ProducerConfig.ACKS_CONFIG] = "all"
            }
        )

        val packet = Packet()

        val metaData: RecordMetadata =
            producer.send(ProducerRecord(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name, packet)).get(5, TimeUnit.SECONDS)
        logger.info("Producer produced to topic@offset -> '$metaData'")

        TestServiceThatAddBigData().also { it.start() }

        val consumer = KafkaConsumer<String, Packet>(
            consumerConfig(
                groupId = "test",
                bootstrapServerUrl = Kafka.instance.bootstrapServers
            ).also {
                it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            }
        )

        consumer.subscribe(listOf(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name))

        TimeUnit.SECONDS.sleep(3)

        val packets = consumer.poll(Duration.ofSeconds(1)).toList()

        packets.size shouldBe 2
        packets.last().value().hasField("big-json")

        producer.close()
    }

    @Test
    fun `Should compress packages with pregenerated json`() {
        val producer = KafkaProducer<String, Packet>(
            producerConfig(
                clientId = "test",
                bootstrapServers = Kafka.instance.bootstrapServers
            ).also {
                it[ProducerConfig.ACKS_CONFIG] = "all"
            }
        )

        val packet = Packet()

        val metaData: RecordMetadata =
            producer.send(ProducerRecord(testTopic.name, packet)).get(5, TimeUnit.SECONDS)
        logger.info("Producer produced to topic@offset -> '$metaData'")

        TestServiceThatAddBigPregeneratedData().also { it.start() }

        val consumer = KafkaConsumer<String, Packet>(
            consumerConfig(
                groupId = "test2",
                bootstrapServerUrl = Kafka.instance.bootstrapServers
            ).also {
                it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            }
        )

        consumer.subscribe(listOf(testTopic.name))

        TimeUnit.SECONDS.sleep(3)

        val packets = consumer.poll(Duration.ofSeconds(1)).toList()

        packets.size shouldBe 2
        packets.last().value().hasField("big-json")

        producer.close()
    }
}
