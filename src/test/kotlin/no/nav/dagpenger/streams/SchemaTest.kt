package no.nav.dagpenger.streams

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SchemaTest {
    @Test
    fun testAppHasAGreeting() {
        val classUnderTest = Topics
        assertNotNull(classUnderTest, "app should have a greeting")
    }
}
