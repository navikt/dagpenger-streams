package no.nav.dagpenger.streams

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.streams.KafkaStreams
import org.junit.jupiter.api.Test

internal class KafkaStreamHealthCheckTest {

    @Test
    fun `Should be up when streams are running`() {
        val streams = mockk<KafkaStreams>().also {
            every { it.state() } returns KafkaStreams.State.RUNNING
        }
        val kafkaStreamHealthCheck = KafkaStreamHealthCheck(streams)

        kafkaStreamHealthCheck.status() shouldBe HealthStatus.UP
    }

    @Test
    fun `Should be up when streams are rebalancing`() {
        val streams = mockk<KafkaStreams>().also {
            every { it.state() } returns KafkaStreams.State.REBALANCING
        }
        val kafkaStreamHealthCheck = KafkaStreamHealthCheck(streams)

        kafkaStreamHealthCheck.status() shouldBe HealthStatus.UP
    }

    @Test
    fun `Should be up when streams are in creation`() {
        val streams = mockk<KafkaStreams>().also {
            every { it.state() } returns KafkaStreams.State.CREATED
        }
        val kafkaStreamHealthCheck = KafkaStreamHealthCheck(streams)

        kafkaStreamHealthCheck.status() shouldBe HealthStatus.UP
    }

    @Test
    fun `Should be down when streams are not running `() {
        val streams = mockk<KafkaStreams>().also {
            every { it.state() } returns KafkaStreams.State.NOT_RUNNING
        }
        val kafkaStreamHealthCheck = KafkaStreamHealthCheck(streams)

        kafkaStreamHealthCheck.status() shouldBe HealthStatus.DOWN
    }

    @Test
    fun `Should be down when streams are in error state `() {
        val streams = mockk<KafkaStreams>().also {
            every { it.state() } returns KafkaStreams.State.ERROR
        }
        val kafkaStreamHealthCheck = KafkaStreamHealthCheck(streams)

        kafkaStreamHealthCheck.status() shouldBe HealthStatus.DOWN
    }

    @Test
    fun `Should be down when streams are in pending shutdown `() {
        val streams = mockk<KafkaStreams>().also {
            every { it.state() } returns KafkaStreams.State.PENDING_SHUTDOWN
        }
        val kafkaStreamHealthCheck = KafkaStreamHealthCheck(streams)

        kafkaStreamHealthCheck.status() shouldBe HealthStatus.DOWN
    }
}
