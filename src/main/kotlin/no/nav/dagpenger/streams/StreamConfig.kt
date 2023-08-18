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
import org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.StreamsConfig.AT_LEAST_ONCE
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import java.io.File
import java.lang.System.getenv
import java.time.Duration
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

private val maxPollRecords = 50
private val maxPollIntervalMs = Duration.ofSeconds(60 + maxPollRecords * 2.toLong()).toMillis()

fun streamConfig(
    appId: String,
    bootStapServerUrl: String,
    credential: KafkaCredential? = null,
    stateDir: String? = null,
    configuration: Configuration = Configuration(),
): Properties {
    return Properties().apply {
        putAll(
            commonProperties(bootStapServerUrl, appId),
        )

        if (Profile.LOCAL != configuration.profile) {
            put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "2")
        }

        stateDir?.let { put(StreamsConfig.STATE_DIR_CONFIG, stateDir) }

        credential?.let { credential ->
            LOGGER.info { "Using user name ${credential.username} to authenticate against Kafka brokers " }
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${credential.username}\" password=\"${credential.password}\";",
            )

            configuration.trustStoreLocation?.let {
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

fun streamConfigAiven(
    appId: String,
    bootStapServerUrl: String,
    stateDir: String? = null,
    aivenCredentials: KafkaAivenCredentials? = null,
    configuration: Configuration = Configuration(),
): Properties {
    return Properties().apply {
        putAll(
            commonProperties(bootStapServerUrl, appId),
        )

        if (Profile.LOCAL != configuration.profile) {
            put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "2")
        }

        stateDir?.let { put(StreamsConfig.STATE_DIR_CONFIG, stateDir) }

        aivenCredentials?.let {
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, it.securityProtocolConfig)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, it.sslEndpointIdentificationAlgorithmConfig)
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, it.sslTruststoreTypeConfig)
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, it.sslKeystoreTypeConfig)
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, it.sslTruststoreLocationConfig)
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, it.sslTruststorePasswordConfig)
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, it.sslKeystoreLocationConfig)
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, it.sslKeystorePasswordConfig)
        }
    }
}

private fun commonProperties(
    bootStapServerUrl: String,
    appId: String,
) = listOf(
    CommonClientConfigs.RETRY_BACKOFF_MS_CONFIG to 1000,
    CommonClientConfigs.RECONNECT_BACKOFF_MS_CONFIG to 5000,
    CommonClientConfigs.MAX_POLL_INTERVAL_MS_CONFIG to "$maxPollIntervalMs",
    StreamsConfig.consumerPrefix(MAX_POLL_RECORDS_CONFIG) to maxPollRecords,
    StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to bootStapServerUrl,
    StreamsConfig.APPLICATION_ID_CONFIG to appId,

    StreamsConfig.COMMIT_INTERVAL_MS_CONFIG to 1,
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
    StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG to LogAndFailExceptionHandler::class.java,

    StreamsConfig.producerPrefix(ProducerConfig.COMPRESSION_TYPE_CONFIG) to "snappy",
    StreamsConfig.producerPrefix(ProducerConfig.BATCH_SIZE_CONFIG) to 32.times(1024)
        .toString(), // 32Kb (default is 16 Kb)

    // Increase max.request.size to 3 MB (default is 1MB )), messages should be compressed but there are currently a bug
    // in kafka-clients ref https://stackoverflow.com/questions/47696396/kafka-broker-is-not-gzipping-my-bigger-size-message-even-though-i-specified-co/48304851#48304851
    StreamsConfig.producerPrefix(ProducerConfig.MAX_REQUEST_SIZE_CONFIG) to 5.times(1024).times(1000).toString(),

    StreamsConfig.PROCESSING_GUARANTEE_CONFIG to AT_LEAST_ONCE,
)

private val localProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "LOCAL",
    ),
)
private val devProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "DEV",

    ),
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "PROD",
    ),
)

data class Configuration(
    val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
    val trustStoreLocation: String? = config().getOrNull(Key("nav.truststore.path", stringType)),
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
