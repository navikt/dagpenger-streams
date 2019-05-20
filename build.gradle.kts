import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    kotlin("jvm") version "1.3.21"
    id("com.diffplug.gradle.spotless") version "3.13.0"
    id("maven-publish")
    id("signing")
    id("io.codearte.nexus-staging") version "0.20.0"
    id("de.marcphilipp.nexus-publish") version "0.1.1"
}

apply {
    plugin("com.diffplug.gradle.spotless")
}

repositories {
    mavenCentral()
    maven("http://packages.confluent.io/maven/")
    maven("https://jitpack.io")
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://dl.bintray.com/kotlin/kotlinx")
    maven("https://dl.bintray.com/kittinunf/maven")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

group = "no.nav.dagpenger"
version = "0.3.7-SNAPSHOT"

val kafkaVersion = "2.0.1"
val confluentVersion = "5.0.2"
val kotlinLoggingVersion = "1.6.22"
val ktorVersion = "1.2.0"
val konfigVersion = "1.6.10.0"
val prometheusVersion = "0.6.0"
val fuelVersion = "1.15.0"
val orgJsonVersion = "20180813"
val jupiterVersion = "5.3.2"
val moshiVersion = "1.8.0"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("no.nav.dagpenger:events:0.3.12-SNAPSHOT")

    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")
    implementation("io.confluent:kafka-streams-avro-serde:$confluentVersion")
    implementation("com.squareup.moshi:moshi-adapters:$moshiVersion")
    implementation("com.squareup.moshi:moshi-kotlin:$moshiVersion")
    implementation("com.squareup.moshi:moshi:$moshiVersion")

    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
    implementation("com.github.kittinunf.fuel:fuel-gson:$fuelVersion")

    implementation("com.natpryce:konfig:$konfigVersion")

    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$jupiterVersion")
    testImplementation("org.json:json:$orgJsonVersion")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
    testImplementation("junit:junit:4.12")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.3.0")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
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
        target("*.gradle.kts", "additionalScripts/*.gradle.kts")
        ktlint()
    }
}
