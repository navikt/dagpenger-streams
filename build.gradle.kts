plugins {
    id("java-library")
    kotlin("jvm") version "1.2.70"
    id("com.diffplug.gradle.spotless") version "3.13.0"
    id("maven-publish")
    id("info.solidsoft.pitest") version "1.3.0"
}

buildscript {
    repositories {
        maven("https://repo.adeo.no/repository/maven-central")
    }
    dependencies {
        classpath("com.cinnober.gradle:semver-git:2.2.0")
    }
}

apply {
    plugin("com.diffplug.gradle.spotless")
    plugin("com.cinnober.gradle.semver-git")
    plugin("info.solidsoft.pitest")
}

repositories {
    maven("https://repo.adeo.no/repository/maven-central")
    maven("http://packages.confluent.io/maven/")
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://dl.bintray.com/kotlin/kotlinx")
    maven("https://dl.bintray.com/kittinunf/maven")
    maven("https://repo.adeo.no/repository/maven-snapshots/")
    maven("https://repo.adeo.no/repository/maven-releases/")
}

group = "no.nav.dagpenger"
version = "0.2.1-SNAPSHOT"

val kafkaVersion = "2.0.0"
val confluentVersion = "5.0.0"
val kotlinLoggingVersion = "1.4.9"
val ktorVersion = "0.9.5"
val prometheusVersion = "0.5.0"
val fuelVersion = "1.15.0"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("no.nav.dagpenger:events:0.1.7-SNAPSHOT")

    api("org.apache.kafka:kafka-clients:$kafkaVersion")
    api("org.apache.kafka:kafka-streams:$kafkaVersion")
    api("io.confluent:kafka-streams-avro-serde:$confluentVersion")

    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-gson:$fuelVersion")

    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.12")
    testImplementation("com.github.tomakehurst:wiremock:2.18.0")
}

publishing {
    publications {
        create("default", MavenPublication::class.java) {
            from(components["java"])
        }
    }

    repositories {
        maven {
            val version = project.version as String

            url = if (version.endsWith("-SNAPSHOT")) {
                uri("https://repo.adeo.no/repository/maven-snapshots/")
            } else {
                uri("https://repo.adeo.no/repository/maven-releases/")
            }
        }
    }
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts", "additionalScripts/*.gradle.kts")
        ktlint()
    }
}

pitest {
    threads = 4
    coverageThreshold = 80
    avoidCallsTo = setOf("kotlin.jvm.internal")
}

tasks.getByName("check").dependsOn("pitest")
