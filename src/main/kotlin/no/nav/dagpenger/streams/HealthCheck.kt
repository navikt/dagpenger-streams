package no.nav.dagpenger.streams

interface HealthCheck {
    val name: String
        get() = this.javaClass.simpleName

    fun status(): HealthStatus
}

enum class HealthStatus {
    UP, DOWN
}
