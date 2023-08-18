import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java-library")
    kotlin("jvm") version Kotlin.version
    id("com.diffplug.spotless") version "6.19.0"
    id("maven-publish")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

kotlin {
    jvmToolchain(17)
}

group = "com.github.navikt"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api(Dagpenger.Events)

    implementation(Kafka.clients)
    implementation(Kafka.streams)
    implementation(Avro.avro)
    implementation(Kafka.Confluent.avroStreamSerdes)
    implementation(Moshi.moshi)
    implementation(Moshi.moshiAdapters)
    implementation(Moshi.moshiKotlin)

    implementation(Konfig.konfig)

    implementation(Kotlin.Logging.kotlinLogging)

    implementation(Ktor2.Server.library("netty"))
    implementation(Ktor2.Server.library("default-headers"))

    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(Micrometer.prometheusRegistry)

    implementation(Log4j2.api)
    implementation(Log4j2.slf4j)

    testImplementation(kotlin("test"))
    testImplementation(Junit5.api)
    testImplementation(KoTest.assertions)
    testImplementation(KoTest.property)
    testImplementation(KoTest.runner)
    testImplementation(Mockk.mockk)
    testImplementation(Ktor2.Server.library("test-host"))
    testImplementation(Kafka.streamTestUtils)
    testImplementation(Json.library)
    testImplementation(TestContainers.kafka)
    testImplementation(Log4j2.core)

    testRuntimeOnly(Junit5.engine)
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

configurations {
    "implementation" {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
    "testImplementation" {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "ch.qos.logback", module = "logback-classic")
    }
}

publishing {
    publications {
        create("mavenJava", MavenPublication::class.java) {
            from(components["java"])
            artifact(sourcesJar.get())

            pom {
                name.set("dagpenger-streams")
                description.set("")
                url.set("https://github.com/navikt/dagpenger-streams")
                withXml {
                    asNode().appendNode("packaging", "jar")
                }
                licenses {
                    license {
                        name.set("MIT License")
                        name.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        organization.set("NAV (Arbeids- og velferdsdirektoratet) - The Norwegian Labour and Welfare Administration")
                        organizationUrl.set("https://www.nav.no")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/navikt/dagpenger-streams.git")
                    developerConnection.set("scm:git:git://github.com/navikt/dagpenger-streams.git")
                    url.set("https://github.com/navikt/dagpenger-streams")
                }
            }
        }
    }
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts", "buildSrc/**/*.kt*")
        ktlint()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}
