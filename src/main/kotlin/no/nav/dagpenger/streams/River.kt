package no.nav.dagpenger.streams

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.Topics.DAGPENGER_BEHOV_PACKET_EVENT
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Predicate

private val LOGGER = KotlinLogging.logger {}

abstract class River : Service() {

    override fun buildTopology(): Topology {
        val builder = StreamsBuilder()
        val stream = builder.consumeTopic(DAGPENGER_BEHOV_PACKET_EVENT)
        stream.peek { key, packet -> LOGGER.info("Processing $packet with key $key") }
            .filter { key, packet -> filterPredicates().all { it.test(key, packet) } }
            .mapValues { _, packet ->
                onPacket(packet)
            }
            .peek { key, packet -> LOGGER.info("Producing $packet with key $key") }
            .toTopic(DAGPENGER_BEHOV_PACKET_EVENT)
        return builder.build()
    }

    abstract fun filterPredicates(): List<Predicate<String, Packet>>
    abstract fun onPacket(packet: Packet): Packet
}
