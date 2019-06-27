package no.nav.dagpenger.plain

import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import mu.KotlinLogging
import no.nav.dagpenger.streams.KafkaCredential
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import java.io.File
import java.util.Properties

fun consumerConfig(
    groupId: String,
    bootstrapServerUrl: String,
    autoOffsetReset: String = "latest",
    enableAutoCommit: Boolean = true,
    keyDeserializer: String? = null,
    valueDeserializer: String? = null,
    credential: KafkaCredential? = null
): Properties {
    return Properties().apply {
        putAll(commonConfig(bootstrapServerUrl, credential))
        put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset)
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit.toString())
        keyDeserializer?.let {
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, it)
        }
        valueDeserializer?.let {
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, it)
        }
    }
}

fun producerConfig(
    clientId: String,
    bootstrapServers: String,
    acks: String = "1",
    keySerializer: String? = null,
    valueSerializer: String? = null,
    credential: KafkaCredential? = null
): Properties {
    return Properties().apply {
        putAll(commonConfig(bootstrapServers, credential))
        put(ProducerConfig.CLIENT_ID_CONFIG, clientId)
        put(ProducerConfig.ACKS_CONFIG, acks)
        keySerializer?.let {
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, it)
        }
        valueSerializer?.let {
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, it)
        }
    }
}

fun commonConfig(bootstrapServers: String, credential: KafkaCredential? = null): Properties {
    return Properties().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        credential?.let { creds ->
            putAll(credentials(creds))
        }
    }
}

private fun credentials(credential: KafkaCredential): Properties {
    return Properties().apply {
        LOGGER.info { "Using user name ${credential.username} to authenticate against Kafka brokers " }
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(
            SaslConfigs.SASL_JAAS_CONFIG,
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${credential.username}\" password=\"${credential.password}\";"
        )

        val trustStoreLocation = System.getenv("NAV_TRUSTSTORE_PATH")
        trustStoreLocation?.let {
            try {
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it).absolutePath)
                put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, System.getenv("NAV_TRUSTSTORE_PASSWORD"))
                LOGGER.info { "Configured '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location " }
            } catch (e: Exception) {
                LOGGER.error { "Failed to set '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location " }
            }
        }
    }
}

private val LOGGER = KotlinLogging.logger {}
