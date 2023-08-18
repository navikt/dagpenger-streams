package no.nav.dagpenger.streams

import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
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
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.Properties
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger { }

private val testTopic = Topic(
    "privat-dagpenger-test",
    keySerde = Serdes.String(),
    valueSerde = Serdes.serdeFrom(PacketSerializer(), PacketDeserializer()),
)

private object Kafka {
    val instance by lazy {
        KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka").withTag("6.1.9")).apply { this.start() }
    }
}

class CompressionPackageTest {

    class TestServiceThatAddBigPregeneratedData : River(testTopic) {
        override val SERVICE_APP_ID = "TestService2"
        override val withHealthChecks = false

        override fun filterPredicates(): List<Predicate<String, Packet>> {
            return listOf(Predicate { _, packet -> !packet.hasField("big-json") })
        }

        override fun onPacket(packet: Packet): Packet {
            val largeJson = this::class.java.getResource("/unormalt-stor-json.json").readText()
            packet.putValue(
                "big-json",
                largeJson,

            )
            return packet
        }

        override fun getConfig(): Properties {
            return streamConfig(SERVICE_APP_ID, Kafka.instance.bootstrapServers)
        }
    }

    @Test
    fun `Should compress packages with pregenerated big json`() {
        val producer = KafkaProducer<String, Packet>(
            producerConfig(
                clientId = "test",
                bootstrapServers = Kafka.instance.bootstrapServers,
            ).also {
                it[ProducerConfig.ACKS_CONFIG] = "all"
            },
        )

        val packet = Packet()

        val metaData: RecordMetadata =
            producer.send(ProducerRecord(testTopic.name, packet)).get(5, TimeUnit.SECONDS)
        logger.info("Producer produced to topic@offset -> '$metaData'")

        TestServiceThatAddBigPregeneratedData().also { it.start() }

        val consumer = KafkaConsumer<String, Packet>(
            consumerConfig(
                groupId = "test2",
                bootstrapServerUrl = Kafka.instance.bootstrapServers,
            ).also {
                it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            },
        )

        consumer.subscribe(listOf(testTopic.name))

        TimeUnit.SECONDS.sleep(3)

        val packets = consumer.poll(Duration.ofSeconds(1)).toList()

        packets.size shouldBe 2
        packets.last().value().hasField("big-json")

        producer.close()
    }
}
