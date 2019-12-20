package no.nav.dagpenger.streams

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.avro.Behov
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced

private val strings = Serdes.String()
private val avroBehovSerde = SpecificAvroSerde<Behov>()
private val genericAvro = GenericAvroSerde()
private val packetSerde = Serdes.serdeFrom(PacketSerializer(), PacketDeserializer())

object Topics {
    val JOARK_EVENTS = Topic(
        "aapen-dok-journalfoering-v1",
        keySerde = strings,
        valueSerde = genericAvro
    )

    @Deprecated(message = "Replaced by 'privat-dagpenger-journalpost-mottatt-v1'", replaceWith = ReplaceWith(
        "Topics.INNGÅENDE_JOURNALPOST_PACKET")
    )
    val INNGÅENDE_JOURNALPOST = Topic(
        "privat-dagpenger-journalpost-mottatt-alpha",
        keySerde = strings,
        valueSerde = avroBehovSerde
    )

    val INNGÅENDE_JOURNALPOST_PACKET_EVENT: Topic<String, Packet> = Topic(
        "privat-dagpenger-journalpost-mottatt-v1",
        keySerde = Serdes.String(),
        valueSerde = packetSerde
    )

    val DAGPENGER_BEHOV_PACKET_EVENT = Topic(
        "privat-dagpenger-behov-v1",
        keySerde = strings,
        valueSerde = packetSerde
    )

    val DAGPENGER_BEHOV_PACKET_EVENT_Q2 = Topic(
        "privat-dagpenger-behov-v1-q2",
        keySerde = strings,
        valueSerde = packetSerde
    )
}

fun <K : Any, V : GenericRecord> StreamsBuilder.consumeGenericTopic(
    topic: Topic<K, V>,
    schemaRegistryUrl: String?
): KStream<K, V> {

    schemaRegistryUrl?.let {
        topic.keySerde.configure(
            mapOf(
                AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl
            ), true
        )

        topic.valueSerde.configure(
            mapOf(
                AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl
            ), false
        )
    }

    return stream<K, V>(
        topic.name, Consumed.with(topic.keySerde, topic.valueSerde)
    )
}

fun <K : Any, V : SpecificRecord> StreamsBuilder.consumeTopic(
    topic: Topic<K, V>,
    schemaRegistryUrl: String?
): KStream<K, V> {

    schemaRegistryUrl?.let {
        topic.keySerde.configure(
            mapOf(
                AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl
            ), true
        )

        topic.valueSerde.configure(
            mapOf(
                AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl
            ), false
        )
    }

    return stream<K, V>(
        topic.name, Consumed.with(topic.keySerde, topic.valueSerde)
    )
}

fun <K : Any, V> StreamsBuilder.consumeTopic(
    topic: Topic<K, V>
): KStream<K, V> {
    return stream<K, V>(
        topic.name, Consumed.with(topic.keySerde, topic.valueSerde)
    )
}

fun <K, V> KStream<K, V>.toTopic(topic: Topic<K, V>) {
    return to(topic.name, Produced.with(topic.keySerde, topic.valueSerde))
}

fun <K, V> KStream<K, V>.toTopic(topic: Topic<K, V>, schemaRegistryUrl: String?) {
    schemaRegistryUrl?.let {
        topic.keySerde.configure(
            mapOf(
                AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl
            ), true
        )

        topic.valueSerde.configure(
            mapOf(
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true,
                AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl
            ), false
        )
    }
    return to(topic.name, Produced.with(topic.keySerde, topic.valueSerde))
}
