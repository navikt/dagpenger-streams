package no.nav.dagpenger.streams

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.streams.Topics.DAGPENGER_BEHOV_PACKET_EVENT
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Predicate

private val LOGGER = KotlinLogging.logger {}

abstract class River : Service() {

    override fun buildTopology(): Topology {
        val builder = StreamsBuilder()
        val stream = builder.consumeTopic(DAGPENGER_BEHOV_PACKET_EVENT)
        stream
            .filterNot { _, packet ->
                if (packet.hasProblem()) {
                    LOGGER.warn { "Skipping packet with problem. Packet $packet" }
                }
                packet.hasProblem()
            }
            .filter { key, packet -> filterPredicates().all { it.test(key, packet) } }
            .mapValues { _, packet ->
                val result = runCatching { onPacket(packet) }
                return@mapValues when {
                    result.isFailure -> {
                        LOGGER.error(result.exceptionOrNull()) { "Failed to process packet" }
                        return@mapValues onFailure(packet)
                    }
                    else -> result.getOrThrow()
                }
            }
            .peek { key, packet -> LOGGER.info("Producing $packet with key $key") }
            .toTopic(DAGPENGER_BEHOV_PACKET_EVENT)
        return builder.build()
    }

    abstract fun filterPredicates(): List<Predicate<String, Packet>>
    abstract fun onPacket(packet: Packet): Packet
    open fun onFailure(packet: Packet): Packet {
        packet.addProblem(
            Problem(
                title = "Ukjent feil ved behandling av Packet"
            )
        )
        return packet
    }
}