package no.nav.dagpenger.streams

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import org.apache.kafka.streams.KafkaStreams
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

abstract class Service {
    protected abstract val SERVICE_APP_ID: String
    protected abstract val HTTP_PORT: Int

    private lateinit var streams: KafkaStreams
    fun start() {
        naisHttpChecks()
        streams = setupStreams()
        streams.start()

        LOGGER.info("Started Service $SERVICE_APP_ID")
        addShutdownHook()
    }

    private fun naisHttpChecks() {
        embeddedServer(Netty, HTTP_PORT) {
            routing {
                get("/isAlive") {
                    call.respondText("ALIVE", ContentType.Text.Plain)
                }
                get("/isReady") {
                    call.respondText("READY", ContentType.Text.Plain)
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        streams.close()
    }

    // Override and extend the set of properties when needed
    fun getConfig(): Properties {
        return streamConfig(SERVICE_APP_ID)
    }

    private fun addShutdownHook() {
        Thread.currentThread().setUncaughtExceptionHandler { _, _ -> stop() }
        Runtime.getRuntime().addShutdownHook(Thread {
            //try {
                stop()
            /*} catch (ignored: Exception) {
            }*/
        })
    }
    protected abstract fun setupStreams(): KafkaStreams
}