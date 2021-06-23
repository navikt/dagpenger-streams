package no.nav.dagpenger.streams

import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import mu.KotlinLogging
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import java.time.Duration
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}
private val bootstrapServersConfig = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092"

abstract class Service {
    protected abstract val SERVICE_APP_ID: String
    protected open val HTTP_PORT: Int = 8080
    protected open val healthChecks: List<HealthCheck> = emptyList()
    protected open val withHealthChecks: Boolean = true
    private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
    private val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, collectorRegistry, Clock.SYSTEM)
    private val streams: KafkaStreams by lazy {
        setupStreamsInternal().also {
            KafkaStreamsMetrics(it).also { metrics -> metrics.bindTo(registry) }
        }
    }

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
        streams.setUncaughtExceptionHandler { exc ->
            logUnexpectedError(exc)
            stop()
            StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT
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
        applicationEngine.stop(gracePeriodMillis = 3000, timeoutMillis = 5000)
    }

    // Override and extend the set of properties when needed
    open fun getConfig(): Properties {
        return streamConfig(SERVICE_APP_ID, bootstrapServersConfig)
    }

    abstract fun buildTopology(): Topology

    private fun addShutdownHooks() {
        Runtime.getRuntime().addShutdownHook(Thread { stop() })
    }

    private fun logUnexpectedError(e: Throwable) {
        when (e) {
            is TopicAuthorizationException -> LOGGER.warn(
                "TopicAuthorizationException in $SERVICE_APP_ID stream, stopping app"
            )
            else -> LOGGER.error(
                "Uncaught exception in $SERVICE_APP_ID stream, thread: ${Thread.currentThread()} message:  ${e.message}",
                e
            )
        }
    }
}
