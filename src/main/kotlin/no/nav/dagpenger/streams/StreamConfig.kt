package no.nav.dagpenger.streams

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import mu.KotlinLogging
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.StreamsConfig.EXACTLY_ONCE
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import java.io.File
import java.lang.System.getenv
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

fun streamConfig(
    appId: String,
    bootStapServerUrl: String,
    credential: KafkaCredential? = null,
    stateDir: String? = null
): Properties {
    return Properties().apply {
        putAll(
                listOf(
                        CommonClientConfigs.RETRY_BACKOFF_MS_CONFIG to 1000,
                        CommonClientConfigs.RECONNECT_BACKOFF_MS_CONFIG to 5000,
                        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to bootStapServerUrl,
                        StreamsConfig.APPLICATION_ID_CONFIG to appId,

                        StreamsConfig.COMMIT_INTERVAL_MS_CONFIG to 1,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                        StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG to LogAndFailExceptionHandler::class.java,

                        ProducerConfig.LINGER_MS_CONFIG to 20,
                        StreamsConfig.POLL_MS_CONFIG to 20
                )
        )

        if (Profile.LOCAL != Configuration().profile) {
            put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, EXACTLY_ONCE)
        }
        stateDir?.let { put(StreamsConfig.STATE_DIR_CONFIG, stateDir) }

        credential?.let { credential ->
            LOGGER.info { "Using user name ${credential.username} to authenticate against Kafka brokers " }
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${credential.username}\" password=\"${credential.password}\";")

            val trustStoreLocation = getenv("NAV_TRUSTSTORE_PATH")
            trustStoreLocation?.let {
                try {
                    put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                    put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it).absolutePath)
                    put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, getenv("NAV_TRUSTSTORE_PASSWORD"))
                    LOGGER.info { "Configured '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location " }
                } catch (e: Exception) {
                    LOGGER.error { "Failed to set '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location " }
                }
            }
        }
    }
}

private val localProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "LOCAL"
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "DEV"

    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "PROD"
    )
)

private data class Configuration(
    val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) }
)

enum class Profile {
    LOCAL, DEV, PROD
}

private fun config() = when (getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
    "dev-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties
    "prod-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties
    else -> {
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
    }
}