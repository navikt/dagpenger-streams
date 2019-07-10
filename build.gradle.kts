import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    kotlin("jvm") version Kotlin.version
    id(Spotless.spotless) version Spotless.version
    id("maven-publish")
}

apply {
    plugin(Spotless.spotless)
}

repositories {
    jcenter()
    maven("http://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

group = "com.github.navikt"

val orgJsonVersion = "20180813"

dependencies {
    implementation(kotlin("stdlib"))
    api(Dagpenger.Events)

    implementation(Kafka.clients)
    implementation(Kafka.streams)
    implementation(Kafka.Confluent.avroStreamSerdes)
    implementation(Moshi.moshi)
    implementation(Moshi.moshiAdapters)
    implementation(Moshi.moshiKotlin)

    implementation(Kotlin.Logging.kotlinLogging)

    implementation(Ktor.serverNetty)

    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(Micrometer.prometheusRegistry)

    implementation(Log4j2.api)

    testImplementation(kotlin("test-junit5"))
    testImplementation(Junit5.api)
    testImplementation(Junit5.kotlinRunner)
    testImplementation(Kafka.streamTestUtils)
    testImplementation(KafkaEmbedded.env)
    testImplementation("org.json:json:$orgJsonVersion")

    testRuntimeOnly(Junit5.engine)
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
        ktlint(Klint.version)
    }
    kotlinGradle {
        target("*.gradle.kts", "additionalScripts/*.gradle.kts")
        ktlint(Klint.version)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
