package no.nav.dagpenger.streams

import org.apache.kafka.common.security.auth.SecurityProtocol

data class KafkaAivenCredentials(
    val securityProtocolConfig: String = SecurityProtocol.SSL.name,
    val sslEndpointIdentificationAlgorithmConfig: String = "",
    val sslTruststoreTypeConfig: String = "jks",
    val sslKeystoreTypeConfig: String = "PKCS12",
    val sslTruststoreLocationConfig: String,
    val sslTruststorePasswordConfig: String,
    val sslKeystoreLocationConfig: String,
    val sslKeystorePasswordConfig: String
)
