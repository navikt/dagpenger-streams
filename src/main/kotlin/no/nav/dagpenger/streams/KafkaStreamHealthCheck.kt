package no.nav.dagpenger.streams

import org.apache.kafka.streams.KafkaStreams

class KafkaStreamHealthCheck(private val streams: KafkaStreams) : HealthCheck {
    override fun status(): HealthStatus {
        return when (streams.state()) {
            KafkaStreams.State.ERROR -> HealthStatus.DOWN
            KafkaStreams.State.PENDING_SHUTDOWN -> HealthStatus.DOWN
            KafkaStreams.State.NOT_RUNNING -> HealthStatus.DOWN
            else -> HealthStatus.UP
        }
    }
}
