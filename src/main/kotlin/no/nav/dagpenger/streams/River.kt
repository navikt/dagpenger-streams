package no.nav.dagpenger.streams

import mu.KotlinLogging
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Predicate
import org.apache.kafka.streams.kstream.Produced

private val LOGGER = KotlinLogging.logger {}

abstract class River : Service() {

    override fun buildTopology(): Topology {
        val builder = StreamsBuilder()
        val stream = builder.stream(
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
            Consumed.with(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde
            )
        )
        stream.peek { key, packet -> LOGGER.info("Processing $packet with key $key") }
            .filter { _, packet -> packet.}
            .filter { key, packet -> filterPredicates().all { it.test(key, packet) } }
            .mapValues { _, packet ->
                try {
                    onPacket(packet)
                } catch (packetException: PacketException) {
                    LOGGER.error("Packet problem: failed to process", packetException)
                    packetException.packet
                }
            }
            .peek { key, packet -> LOGGER.info("Producing $packet with key $key") }
            .to(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Produced.with(
                    Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde,
                    Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde
                )
            )

        return builder.build()
    }

    abstract fun filterPredicates(): List<Predicate<String, Packet>>
    abstract fun onPacket(packet: Packet): Packet
}
