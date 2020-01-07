package no.nav.dagpenger.streams

import io.kotlintest.shouldBe
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
import org.apache.kafka.streams.kstream.Predicate
import org.junit.jupiter.api.Test
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import java.time.Duration
import java.util.Properties
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger { }
class CompressionPackageTest {

    private object Kafka {
        val instance by lazy {
            KafkaContainer("5.3.0").apply { this.start() }
        }
    }

    class TestServiceThatAddBigData : River(Topics.DAGPENGER_BEHOV_PACKET_EVENT) {
        override val SERVICE_APP_ID = "TestService"

        override fun filterPredicates(): List<Predicate<String, Packet>> {
            return listOf(Predicate { _, packet -> !packet.hasField("big") })
        }

        override fun onPacket(packet: Packet): Packet {
            packet.putValue("big", RandomStringUtils.random(2000000, "ABC"))
            return packet
        }

        override fun getConfig(): Properties {
            return streamConfig(SERVICE_APP_ID, Kafka.instance.bootstrapServers)
        }
    }

    @Test
    fun `Should compress packages`() {
        val producer = KafkaProducer<String, Packet>(
            producerConfig(
                clientId = "test",
                bootstrapServers = Kafka.instance.bootstrapServers
            ).also {
                it[ProducerConfig.ACKS_CONFIG] = "all"
            })

        val packet = Packet()

        val metaData: RecordMetadata = producer.send(ProducerRecord(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name, packet)).get(5, TimeUnit.SECONDS)
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
        packets.last().value().hasField("big")
        packets.last().value().getStringValue("big").length shouldBe 2000000
    }
}