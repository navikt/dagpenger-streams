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
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.function.Predicate

private val LOGGER = KotlinLogging.logger {}

abstract class RiverConsumer : ConsumerService() {
    val reproducer: KafkaProducer<String, Packet> = KafkaProducer(streamConfig(SERVICE_APP_ID, bootstrapServersConfig))

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            reproducer.close(5, TimeUnit.SECONDS)
        })
    }

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
                    }.forEach { (key, packet) ->
                        reproducer.send(ProducerRecord(Topics.DAGPENGER_BEHOV_PACKET_EVENT.name, key, packet))
                    }
            }
        }
    }

    abstract fun filterPredicates(): List<Predicate<ConsumerRecord<String, Packet>>>
    abstract fun onPacket(packet: Packet): Packet
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