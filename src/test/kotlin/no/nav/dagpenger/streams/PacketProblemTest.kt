package no.nav.dagpenger.streams

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class PacketProblemsTest {

    private val validJson = "{\"key1\":\"value1\"}"
    private var problems = PacketProblems(validJson)

    @Test
    fun `no problems in new instance`() {
        assertFalse(problems.hasErrors())
    }

    @Test fun `errors detected`() {
        problems.error("Simple error")
        assertTrue(problems.hasErrors())
        assertThat(problems.toString(), containsString("Errors"))
        assertThat(problems.toString(), containsString("Simple error"))
    }
}