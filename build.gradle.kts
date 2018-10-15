plugins {
    id("java-library")
    kotlin("jvm") version "1.2.51"
    id("com.diffplug.gradle.spotless") version "3.13.0"
    id("maven-publish")
}

buildscript {
    dependencies {
        classpath("com.cinnober.gradle:semver-git:2.2.0")
    }
}

apply {
    plugin("com.diffplug.gradle.spotless")
    plugin("com.cinnober.gradle.semver-git")
}

repositories {
    jcenter()
    maven(url = "http://packages.confluent.io/maven/")
    maven(url = "https://dl.bintray.com/kotlin/ktor")
    maven(url = "https://repo.adeo.no/repository/maven-snapshots/")
    maven(url = "https://repo.adeo.no/repository/maven-releases/")
}

group = "no.nav.dagpenger"
version = "0.1.4-SNAPSHOT"

val kafkaVersion = "2.0.0"
val confluentVersion = "5.0.0"
val kotlinLoggingVersion = "1.4.9"
val ktorVersion = "0.9.5"
val prometheusVersion = "0.5.0"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("no.nav.dagpenger:events:0.1.5-SNAPSHOT")

    api("org.apache.kafka:kafka-clients:$kafkaVersion")
    api("org.apache.kafka:kafka-streams:$kafkaVersion")
    api("io.confluent:kafka-streams-avro-serde:$confluentVersion")

    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.12")
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
