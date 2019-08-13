package no.nav.dagpenger.streams

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.util.Properties

class ServiceTest {

    class ServiceUnderTestWithoutHealthChecks : Service() {
        override fun buildTopology(): Topology = StreamsBuilder().build()
        override val SERVICE_APP_ID: String = "under-test"
        override val withHealthChecks = false
        override val HTTP_PORT: Int = getAvailablePort()

        override fun getConfig(): Properties {
            return streamConfig(SERVICE_APP_ID, "localhost:${getAvailablePort()}", KafkaCredential("bla", "bla"))
        }
        fun getPort(): Int { return HTTP_PORT }
    }

    companion object {

        val serviceUnderTest = ServiceUnderTestWithoutHealthChecks()

        @BeforeAll
        @JvmStatic
        fun setup() {
            serviceUnderTest.start()
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            serviceUnderTest.stop()
        }

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
        val port = serviceUnderTest.getPort()
        shouldThrow<ConnectException> {
            assert200okUrl("http://localhost:$port/isAlive")
        }
        shouldThrow<ConnectException> {
            assert200okUrl("http://localhost:$port/isReady")
        }
        shouldThrow<ConnectException> {
            assert200okUrl("http://localhost:$port/metrics")
        }
    }

    private fun assert200okUrl(urlString: String) {
        val url = URL(urlString)
        val con = url.openConnection() as HttpURLConnection

        val responseCode = con.responseCode

        responseCode shouldBe 200
    }
}