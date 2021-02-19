package no.nav.dagpenger.streams

object Helpers {
    val topicName = Topics.DAGPENGER_BEHOV_PACKET_EVENT.name
    val keySerializer = Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.serializer()
    val valueSerializer = Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.serializer()
    val keyDeSerializer = Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer()
    val valueDeSerializer = Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
}
