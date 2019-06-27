package no.nav.dagpenger.plain

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.streams.Topics
import no.nav.dagpenger.streams.processTimeLatency
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.errors.RetriableException
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.function.Predicate

private val LOGGER = KotlinLogging.logger {}

abstract class RiverConsumer : ConsumerService() {
    val reproducer: KafkaProducer<String, Packet> = KafkaProducer(producerConfig(
        clientId = SERVICE_APP_ID,
        bootstrapServers = bootstrapServersConfig),
        Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.serializer(),
        Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.serializer()
        )

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            LOGGER.info("Closing $SERVICE_APP_ID Kafka producer")
            reproducer.flush()
            reproducer.close()
            LOGGER.info("done! ")
        })
    }

    override suspend fun run() {
        KafkaConsumer<String, Packet>(
            consumerConfig(groupId = SERVICE_APP_ID, bootstrapServerUrl = bootstrapServersConfig),
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
        ).use { consumer ->
            consumer.subscribe(listOf(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name))
            while (job.isActive) {
                try {
                    val records = consumer.poll(Duration.of(100, ChronoUnit.MILLIS))

                    records.asSequence()
                        .onEach { r -> LOGGER.info("Pond recieved packet with key ${r.key()} and will test it against filters.") }
                        .filterNot { r -> r.value().hasProblem() }
                        .filter { r -> filterPredicates().all { p -> p.test(r) } }
                        .map { r ->
                            val result = runCatching {
                                val timer = processTimeLatency.startTimer()
                                try {
                                    onPacket(r.value())
                                } finally {
                                    timer.observeDuration()
                                }
                            }
                            when {
                                result.isFailure -> {
                                    LOGGER.error(result.exceptionOrNull()) { "Failed to process packet ${r.value()}" }
                                    return@map r.key() to onFailure(r.value(), result.exceptionOrNull())
                                }
                                else -> r.key() to result.getOrThrow()
                            }
                        }.onEach { (key, packet) ->
                            LOGGER.info { "Producing packet with key $key and value: $packet" }
                        }.forEach { (key, packet) -> produceEvent(key, packet) }
                } catch (e: RetriableException) {
                    LOGGER.warn("Kafka threw a retriable exception. Will retry", e)
                }
            }
        }
    }

    abstract fun filterPredicates(): List<Predicate<ConsumerRecord<String, Packet>>>
    abstract fun onPacket(packet: Packet): Packet
    open fun produceEvent(key: String, packet: Packet): Future<RecordMetadata> {
        return reproducer.send(
            ProducerRecord(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                key,
                packet
            )
        ) { metadata, exception ->
            exception?.let { LOGGER.error { "Failed to produce Packet" } }
            metadata?.let { LOGGER.info { "Produced Packet on topic ${metadata.topic()} to offset ${metadata.offset()} with the key ${key}" } }
        }
    }

    open fun onFailure(packet: Packet, error: Throwable?): Packet {
        packet.addProblem(
            Problem(
                title = "Ukjent feil ved behandling av Packet"
            )
        )
        return packet
    }

    override fun shutdown() {
        reproducer.close(5, TimeUnit.SECONDS)
    }
}