package no.nav.dagpenger.streams

import org.apache.kafka.common.security.auth.SecurityProtocol

data class KafkaAivenCredentials(
    val securityProtocolConfig: String = SecurityProtocol.SSL.name,
    val sslEndpointIdentificationAlgorithmConfig: String = "",
    val sslTruststoreTypeConfig: String = "jks",
    val sslKeystoreTypeConfig: String = "PKCS12",
    val sslTruststoreLocationConfig: String = "/var/run/secrets/nais.io/kafka/client.truststore.jks",
    val sslTruststorePasswordConfig: String = System.getenv("KAFKA_CREDSTORE_PASSWORD"),
    val sslKeystoreLocationConfig: String = "/var/run/secrets/nais.io/kafka/client.keystore.p12",
    val sslKeystorePasswordConfig: String = sslTruststorePasswordConfig
)
