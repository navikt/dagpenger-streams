package no.nav.dagpenger.streams

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import mu.KotlinLogging
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.Topology
import java.util.Properties
import java.util.concurrent.TimeUnit

private val LOGGER = KotlinLogging.logger {}
private val bootstrapServersConfig = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092"

abstract class Service {
    protected abstract val SERVICE_APP_ID: String
    protected open val HTTP_PORT: Int = 8080
    private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry

    private lateinit var streams: KafkaStreams
    private lateinit var applicationEngine: ApplicationEngine
    fun start() {
        DefaultExports.initialize()
        applicationEngine = naisHttpChecks()
        applicationEngine.start(wait = false)
        streams = setupStreamsInternal()
        streams.start()
        LOGGER.info("Started Service $SERVICE_APP_ID")
        addShutdownHooks()
    }

    private fun setupStreamsInternal(): KafkaStreams {
        val streams = KafkaStreams(buildTopology(), getConfig())
        streams.setUncaughtExceptionHandler { t, e ->
            logUnexpectedError(t, e)
            stop()
        }
        return streams
    }

    private fun naisHttpChecks(): ApplicationEngine {
        return embeddedServer(Netty, HTTP_PORT) {
            routing {
                get("/isAlive") {
                    call.respondText("ALIVE", ContentType.Text.Plain)
                }
                get("/isReady") {
                    call.respondText("READY", ContentType.Text.Plain)
                }
                get("/metrics") {
                    val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: setOf()
                    call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                        TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
                    }
                }
            }
        }
    }

    fun stop() {
        LOGGER.info { "Shutting down $SERVICE_APP_ID" }
        streams.close(3, TimeUnit.SECONDS)
        streams.cleanUp()
        applicationEngine.stop(gracePeriod = 3, timeout = 5, timeUnit = TimeUnit.SECONDS)
    }

    // Override and extend the set of properties when needed
    open fun getConfig(): Properties {
        return streamConfig(SERVICE_APP_ID, bootstrapServersConfig)
    }

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

    abstract fun buildTopology(): Topology
}
