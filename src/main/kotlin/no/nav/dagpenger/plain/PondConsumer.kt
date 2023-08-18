package no.nav.dagpenger.plain

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.RetriableException
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.function.Predicate

private val LOGGER = KotlinLogging.logger {}

abstract class PondConsumer(brokerUrl: String) : ConsumerService(brokerUrl) {
    override fun run() {
        KafkaConsumer<String, Packet>(
            getConsumerConfig(),
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer(),
        ).use { consumer ->
            consumer.subscribe(listOf(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name))
            while (job.isActive) {
                try {
                    val records = consumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                    records.asSequence()
                        .onEach { r -> LOGGER.info("Pond recieved packet with ${r.key()} and will test it against filters.") }
                        .filterNot { r -> r.value().hasProblem() }
                        .filter { r -> filterPredicates().all { p -> p.test(r.value()) } }
                        .forEach { r -> onPacket(r.value()) }
                } catch (e: RetriableException) {
                    LOGGER.warn("Kafka threw a retriable exception, will retry", e)
                }
            }
        }
    }

    abstract fun filterPredicates(): List<Predicate<Packet>>
    abstract fun onPacket(packet: Packet)
    override fun shutdown() {
    }
}
