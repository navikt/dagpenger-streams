package no.nav.dagpenger.streams

import io.kotlintest.shouldBe
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.io.IOException
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.util.Properties

class ServiceTest {

    class ServiceUnderTest(httpPort: Int) : Service() {
        override fun buildTopology(): Topology = StreamsBuilder().build()

        override val SERVICE_APP_ID: String = "under-test"
        override val HTTP_PORT: Int = httpPort

        override fun getConfig(): Properties {
            return streamConfig(SERVICE_APP_ID, "localhost:${getAvailablePort()}", KafkaCredential("bla", "bla"))
        }
    }

    companion object {
        val httpPort = getAvailablePort()

        val serviceUnderTest = ServiceUnderTest(httpPort)

        @BeforeClass
        @JvmStatic
        fun setup() {
            serviceUnderTest.start()
        }

        @AfterClass
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
    fun `Should have http alive check`() {
        assert200okUrl("http://localhost:$httpPort/isAlive")
    }

    @Test
    fun `Should have http ready check`() {
        assert200okUrl("http://localhost:$httpPort/isReady")
    }

    @Test
    fun `Should have http metrics endpoint check`() {
        assert200okUrl("http://localhost:$httpPort/metrics")
    }

    private fun assert200okUrl(urlString: String) {
        val url = URL(urlString)
        val con = url.openConnection() as HttpURLConnection

        // optional default is GET
        con.requestMethod = "GET"

        val responseCode = con.responseCode

        responseCode shouldBe 200
    }
}