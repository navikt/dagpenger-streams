package no.nav.dagpenger.streams

import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.httpGet
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.io.IOException
import java.net.ServerSocket
import java.util.Properties
import kotlin.test.assertTrue

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
        val (_, response, result) = with("http://localhost:$httpPort/isAlive".httpGet()) {
            responseString()
        }

        assertTrue("should have isAlive endpoint but was $result") { response.isSuccessful }
    }

    @Test
    fun `Should have http ready check`() {
        val (_, response, result) = with("http://localhost:$httpPort/isReady".httpGet()) {
            responseString()
        }

        assertTrue("should have isReady endpoint but was $result") { response.isSuccessful }
    }

    @Test
    fun `Should have http metrics endpoint check`() {
        val (_, response, result) = with("http://localhost:$httpPort/metrics".httpGet()) {
            responseString()
        }

        assertTrue("should have metrics endpoint but was $result") { response.isSuccessful }
    }
}