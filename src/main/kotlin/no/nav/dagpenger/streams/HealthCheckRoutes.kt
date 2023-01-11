package no.nav.dagpenger.streams

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import mu.KotlinLogging

private val LOGGER = KotlinLogging.logger {}

internal fun Application.health(healthChecks: List<HealthCheck>) {
    install(DefaultHeaders)
    routing {
        healthRoutes(healthChecks)
    }
}

fun Route.healthRoutes(healthChecks: List<HealthCheck>) {
    route("/metrics") {
        get {
            val names = call.request.queryParameters.getAll("name")?.toSet() ?: kotlin.collections.emptySet()
            call.respondTextWriter(
                ContentType.parse(TextFormat.CONTENT_TYPE_004),
                HttpStatusCode.OK
            ) {
                TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
            }
        }
    }

    route("/isAlive") {
        get {
            val failedHealthChecks = healthChecks.filter {
                it.status() == HealthStatus.DOWN
            }
            if (failedHealthChecks.isNotEmpty()) {
                failedHealthChecks.forEach {
                    LOGGER.warn { "Health check '${it.name}' failed" }
                }
                call.respondText("ERROR", ContentType.Text.Plain, HttpStatusCode.ServiceUnavailable)
            } else {
                call.respondText("ALIVE", ContentType.Text.Plain)
            }
        }
    }
    route("/isReady") {
        get {
            call.respondText(text = "READY", contentType = io.ktor.http.ContentType.Text.Plain)
        }
    }
}
