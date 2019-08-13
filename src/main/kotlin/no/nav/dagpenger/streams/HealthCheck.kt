package no.nav.dagpenger.streams

interface HealthCheck {
    fun status(): HealthStatus
}

enum class HealthStatus {
    UP, DOWN
}
