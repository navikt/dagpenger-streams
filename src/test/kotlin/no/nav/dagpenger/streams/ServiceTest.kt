package no.nav.dagpenger.streams

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.util.Properties
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.junit.jupiter.api.Test

class ServiceTest {

    class ServiceUnderTest(override val withHealthChecks: Boolean, override val healthChecks: List<HealthCheck>) :
        Service() {
        override fun buildTopology(): Topology = StreamsBuilder().build()
        override val SERVICE_APP_ID: String = "under-test"
        override val HTTP_PORT: Int = getAvailablePort()
        override fun getConfig(): Properties {
            return streamConfig(SERVICE_APP_ID, "localhost:${getAvailablePort()}", KafkaCredential("bla", "bla"))
        }

        fun getPort(): Int {
            return HTTP_PORT
        }
    }

    companion object {

        fun getAvailablePort(): Int =
            try {
                ServerSocket(0).run {
                    reuseAddress = true
                    close()
                    localPort
                }
            } catch (e: IOException) {
                0
            }
    }

    @Test
    fun `Should be able to turn off health check and metrics api`() {
        val serviceUnderTest = ServiceUnderTest(withHealthChecks = false, healthChecks = emptyList())
        serviceUnderTest.start()
        val port = serviceUnderTest.getPort()
        shouldThrow<ConnectException> {
            assertUrl(url = "http://localhost:$port/isAlive", status = 200)
        }
        shouldThrow<ConnectException> {
            assertUrl(url = "http://localhost:$port/isReady", status = 200)
        }
        shouldThrow<ConnectException> {
            assertUrl(url = "http://localhost:$port/metrics", status = 200)
        }
        serviceUnderTest.stop()
    }

    @Test
    fun `Should be able to add configurable health checks to service`() {
        val serviceUnderTest = ServiceUnderTest(
            withHealthChecks = true, healthChecks = listOf(
                object : HealthCheck {
                    override fun status(): HealthStatus {
                        return HealthStatus.DOWN
                    }

                    override val name: String
                        get() = "FailedHealthCheck"
                }
            ))
        serviceUnderTest.start()
        val port = serviceUnderTest.getPort()
        assertUrl(url = "http://localhost:$port/isAlive", status = 503)
        serviceUnderTest.stop()
    }

    private fun assertUrl(url: String, status: Int) {
        val con = URL(url).openConnection() as HttpURLConnection
        val responseCode = con.responseCode
        responseCode shouldBe status
    }
}
