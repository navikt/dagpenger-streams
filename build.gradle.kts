import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    kotlin("jvm") version Kotlin.version
    id(Spotless.spotless) version Spotless.version
    id("maven-publish")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
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

    implementation(Ktor.serverNetty)

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
    testImplementation(Ktor.ktorTest)
    testImplementation(Kafka.streamTestUtils)
    testImplementation(Json.library)
    testImplementation(TestContainers.kafka)

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
        ktlint(Ktlint.version)
    }
    kotlinGradle {
        target("*.gradle.kts", "buildSrc/**/*.kt*")
        ktlint(Ktlint.version)
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
