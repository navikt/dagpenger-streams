package no.nav.dagpenger.plain

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.Topics
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.function.Predicate

private val LOGGER = KotlinLogging.logger {}

abstract class PondConsumer : ConsumerService() {
    override suspend fun run() {
        KafkaConsumer<String, Packet>(
            streamConfig(SERVICE_APP_ID, bootstrapServersConfig),
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
        ).use { consumer ->
            consumer.subscribe(listOf(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name))
            while (job.isActive) {
                val records = consumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                records.asSequence()
                    .onEach { r -> LOGGER.info("Pond recieved packet with ${r.key()} and will test it against filters.") }
                    .filterNot { r -> r.value().hasProblem() }
                    .filter { r -> filterPredicates().all { p -> p.test(r) } }
                    .forEach { r -> onPacket(r.value()) }
            }
        }
    }

    abstract fun filterPredicates(): List<Predicate<ConsumerRecord<String, Packet>>>
    abstract fun onPacket(packet: Packet)
    override fun shutdown() {
    }
}