package no.nav.dagpenger.streams

import mu.KotlinLogging
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import java.io.File
import java.lang.System.getenv
import java.util.Properties

private val bootstrapServersConfig = getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092"
private val kafkaUserName: String? = getenv("KAFKA_USERNAME")
private val kafkaUserPassword: String? = getenv("KAFKA_PASSWORD")

private val LOGGER = KotlinLogging.logger {}

fun streamConfig(
    appId: String,
    stateDir: String? = null
): Properties {
    return Properties().apply {
        putAll(
                listOf(
                        CommonClientConfigs.RETRY_BACKOFF_MS_CONFIG to 1000,
                        CommonClientConfigs.RECONNECT_BACKOFF_MS_CONFIG to 5000,
                        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServersConfig,
                        StreamsConfig.APPLICATION_ID_CONFIG to appId,
                        // TODO Using processing guarantee requires replication of 3, not possible with current single node dev environment
                        //StreamsConfig.PROCESSING_GUARANTEE_CONFIG to "exactly_once",
                        StreamsConfig.COMMIT_INTERVAL_MS_CONFIG to 1,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                        StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG to LogAndFailExceptionHandler::class.java
                )
        )

        stateDir?.let { put(StreamsConfig.STATE_DIR_CONFIG, stateDir) }

        kafkaUserName?.let { name ->
            kafkaUserPassword.let { pwd ->
                LOGGER.info { "Using user name $name to authenticate against Kafka brokers " }
                put(SaslConfigs.SASL_MECHANISM, "PLAIN")
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$name\" password=\"$pwd\";")

                val trustStoreLocation = getenv("NAV_TRUSTSTORE_PATH")
                trustStoreLocation?.let { ClassLoader.getSystemResource(it) }?.let {
                    try {
                        put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it.toURI()).absolutePath)
                        put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, getenv("NAV_TRUSTSTORE_PASSWORD"))
                        LOGGER.info { "Configured '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location " }
                    } catch (e: Exception) {
                        LOGGER.error { "Failed to set '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location " }
                    }
                }
            }
        }
    }
}
