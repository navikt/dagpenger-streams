package no.nav.dagpenger.streams

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.Problem
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Predicate
import org.apache.logging.log4j.ThreadContext

private val LOGGER = KotlinLogging.logger {}

@Deprecated("Bruk rapid & rivers i stedet - https://github.com/navikt/rapids-and-rivers")
abstract class River(private val topic: Topic<String, Packet>) : Service() {

    override fun buildTopology(): Topology {
        val builder = StreamsBuilder()
        val stream = builder.consumeTopic(topic)

        stream
            .peek { _, packet -> ThreadContext.put(CorrelationId.X_CORRELATION_ID, packet.getCorrelationId()) }
            .peek { key, _ -> LOGGER.debug { "River recieved packet with key $key and will test it against filters." } }
            .filterNot { _, packet -> packet.hasProblem() }
            .filter { key, packet -> filterPredicates().all { it.test(key, packet) } }
            .mapValues { key, packet ->
                LOGGER.debug { "Packet with key $key passed filters and now calling onPacket() for: $packet" }

                val result = runCatching {
                    val timer = processTimeLatency.startTimer()
                    try {
                        onPacket(packet)
                    } finally {
                        timer.observeDuration()
                    }
                }
                return@mapValues result.fold(
                    { it },
                    { throwable ->
                        LOGGER.error(throwable) { "Failed to process packet $packet" }
                        return@mapValues onFailure(packet, throwable)
                    }
                )
            }
            .peek { key, packet -> LOGGER.debug { "Producing packet with key $key and value: $packet" } }
            .peek { _, _ -> ThreadContext.remove(CorrelationId.X_CORRELATION_ID) }
            .toTopic(topic)
        return builder.build()
    }

    abstract fun filterPredicates(): List<Predicate<String, Packet>>
    abstract fun onPacket(packet: Packet): Packet

    open fun onFailure(packet: Packet, error: Throwable?): Packet {
        packet.addProblem(
            Problem(
                title = "Ukjent feil ved behandling av Packet"
            )
        )
        return packet
    }
}
