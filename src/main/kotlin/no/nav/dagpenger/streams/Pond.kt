package no.nav.dagpenger.streams

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Predicate
import org.apache.logging.log4j.ThreadContext

private val LOGGER = KotlinLogging.logger {}

abstract class Pond(private val topic: Topic<String, Packet>) : Service() {

    override fun buildTopology(): Topology {
        val builder = StreamsBuilder()
        val stream = builder.consumeTopic(topic)
        stream
            .peek { _, packet -> ThreadContext.put(CorrelationId.X_CORRELATION_ID, packet.getCorrelationId()) }
            .peek { key, _ -> LOGGER.debug { "Pond recieved packet with key $key and will test it against filters." } }
            .filter { key, packet -> filterPredicates().all { it.test(key, packet) } }
            .foreach { key, packet ->
                LOGGER.debug { "Packet with key $key passed filters and now calling onPacket() for: $packet" }

                val timer = processTimeLatency.startTimer()
                onPacket(packet)
                timer.observeDuration()
                ThreadContext.remove(CorrelationId.X_CORRELATION_ID)
            }
        return builder.build()
    }

    abstract fun filterPredicates(): List<Predicate<String, Packet>>
    abstract fun onPacket(packet: Packet)
}
