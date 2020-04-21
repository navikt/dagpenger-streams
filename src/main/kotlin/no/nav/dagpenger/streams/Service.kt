package no.nav.dagpenger.streams

import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.kafka.KafkaConsumerMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import java.time.Duration
import java.util.Properties
import java.util.concurrent.TimeUnit
import mu.KotlinLogging
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.Topology

private val LOGGER = KotlinLogging.logger {}
private val bootstrapServersConfig = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092"

abstract class Service {
    protected abstract val SERVICE_APP_ID: String
    protected open val HTTP_PORT: Int = 8080
    protected open val healthChecks: List<HealthCheck> = emptyList()
    protected open val withHealthChecks: Boolean = true
    private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
    private val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, collectorRegistry, Clock.SYSTEM)
    private val kafkaConsumerMetrics = KafkaConsumerMetrics().also {
        it.bindTo(registry)
    }
    private val streams: KafkaStreams by lazy { setupStreamsInternal() }

    private val applicationEngine: ApplicationEngine by lazy {
        naisHttpChecks(healthChecks + KafkaStreamHealthCheck(streams))
    }

    fun start() {
        DefaultExports.initialize()
        streams.start()
        if (withHealthChecks) {
            applicationEngine.start(wait = false)
        }
        LOGGER.info("Started Service $SERVICE_APP_ID on http port $HTTP_PORT")
        addShutdownHooks()
    }

    private fun setupStreamsInternal(): KafkaStreams {
        LOGGER.info("Setting up topology for $SERVICE_APP_ID")
        val streams = KafkaStreams(buildTopology(), getConfig())
        streams.setUncaughtExceptionHandler { t, e ->
            logUnexpectedError(t, e)
            stop()
        }
        return streams
    }

    private fun naisHttpChecks(healthChecks: List<HealthCheck>): ApplicationEngine {
        return embeddedServer(Netty, HTTP_PORT) {
            health(healthChecks)
        }
    }

    fun stop() {
        LOGGER.info { "Shutting down $SERVICE_APP_ID" }
        streams.close(Duration.ofSeconds(3))
        streams.cleanUp()
        applicationEngine.stop(gracePeriod = 1, timeout = 5, timeUnit = TimeUnit.SECONDS)
    }

    // Override and extend the set of properties when needed
    open fun getConfig(): Properties {
        return streamConfig(SERVICE_APP_ID, bootstrapServersConfig)
    }

    abstract fun buildTopology(): Topology

    private fun addShutdownHooks() {
        Thread.currentThread().setUncaughtExceptionHandler { t, e ->
            logUnexpectedError(t, e)
            stop()
        }
        Runtime.getRuntime().addShutdownHook(Thread { stop() })
    }

    private fun logUnexpectedError(t: Thread?, e: Throwable) {
        LOGGER.error(
            "Uncaught exception in $SERVICE_APP_ID stream, thread: $t message:  ${e.message}", e
        )
    }
}
