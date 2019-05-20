package no.nav.dagpenger

import io.kotlintest.matchers.maps.shouldNotContainKey
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.StreamsConfig
import org.junit.jupiter.api.Test

class StreamConfigTest {

    @Test
    fun `Should have not have configured PROCESSING_GUARANTEE_CONFIG to EXACTLY_ONCE on local profile `() {

        val properties = streamConfig("test-app", "localhost:9093")
        properties shouldNotContainKey StreamsConfig.PROCESSING_GUARANTEE_CONFIG
    }
}