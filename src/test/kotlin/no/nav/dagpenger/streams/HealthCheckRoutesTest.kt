package no.nav.dagpenger.streams

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class HealthApplicationTest {

    private val goodHealthChecks: List<HealthCheck>
        get() = listOf(
            mockk<HealthCheck>().also {
                every { it.status() } returns HealthStatus.UP
            }
        )

    @Test
    fun `Should have alive, ready and metrics endpoints`() = test {
        client.get("/isAlive").status shouldBe HttpStatusCode.OK
        client.get("/isReady").status shouldBe HttpStatusCode.OK
        client.get("/metrics").status shouldBe HttpStatusCode.OK
    }

    private val badHealthChecks: List<HealthCheck>
        get() = listOf(
            mockk<HealthCheck>().also {
                every { it.status() } returns HealthStatus.DOWN
            }
        )

    @Test
    fun `alive check should fail if a healtcheck is down `() = test(api(badHealthChecks)) {
        client.get("/isAlive").status shouldBe HttpStatusCode.ServiceUnavailable
    }

    private fun test(
        moduleFunction: Application.() -> Unit = api(goodHealthChecks),
        test: suspend ApplicationTestBuilder.() -> Unit
    ) {

        return testApplication {
            application(moduleFunction)
            test()
        }
    }
}

private fun api(
    healthChecks: List<HealthCheck>
): Application.() -> Unit {
    return fun Application.() {
        health(
            healthChecks
        )
    }
}
