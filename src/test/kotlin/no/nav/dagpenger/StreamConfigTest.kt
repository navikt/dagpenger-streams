package no.nav.dagpenger

import io.kotlintest.matchers.maps.shouldContainKey
import io.kotlintest.matchers.maps.shouldContainValue
import io.kotlintest.matchers.maps.shouldNotContainKey
import io.kotlintest.matchers.maps.shouldNotContainValue
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.StreamsConfig
import org.junit.jupiter.api.Test

class StreamConfigTest {

    @Test
    fun `Should not have configured PROCESSING_GUARANTEE_CONFIG to EXACTLY_ONCE on local profile `() {

        val properties = streamConfig("test-app", "localhost:9093")
        properties shouldNotContainKey StreamsConfig.PROCESSING_GUARANTEE_CONFIG
        properties shouldNotContainValue StreamsConfig.EXACTLY_ONCE
    }

    @Test
    fun `Should  have configured PROCESSING_GUARANTEE_CONFIG to EXACTLY_ONCE on dev profile `() {

        System.setProperty("NAIS_CLUSTER_NAME", "dev-fss")
        val properties = streamConfig("test-app", "localhost:9093")
        properties shouldContainKey StreamsConfig.PROCESSING_GUARANTEE_CONFIG
        properties shouldContainValue StreamsConfig.EXACTLY_ONCE
        System.clearProperty("NAIS_CLUSTER_NAME")
    }

    @Test
    fun `Should  have configured PROCESSING_GUARANTEE_CONFIG to EXACTLY_ONCE on prod profile `() {

        System.setProperty("NAIS_CLUSTER_NAME", "prod-fss")
        val properties = streamConfig("test-app", "localhost:9093")
        properties shouldContainKey StreamsConfig.PROCESSING_GUARANTEE_CONFIG
        properties shouldContainValue StreamsConfig.EXACTLY_ONCE
        System.clearProperty("NAIS_CLUSTER_NAME")
    }
}