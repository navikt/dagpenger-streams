package no.nav.dagpenger.streams

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.dagpenger.events.avro.Behov
import no.nav.dagpenger.events.avro.Regelbehov
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced

private val strings = Serdes.String()
private val avroBehovSerde = SpecificAvroSerde<Behov>()
private val avroRegelbehovSerde = SpecificAvroSerde<Regelbehov>()
private val genericAvro = GenericAvroSerde()

object Topics {
    val JOARK_EVENTS = Topic(
        "aapen-dok-journalfoering-v1",
        keySerde = strings,
        valueSerde = genericAvro
    )

    val INNGÅENDE_JOURNALPOST = Topic(
        "privat-dagpenger-journalpost-mottatt-alpha",
        keySerde = strings,
        valueSerde = avroBehovSerde
    )

    val VILKÅR_EVENT = Topic(
        "privat-dagpenger-vilkar",
        keySerde = strings,
        valueSerde = avroRegelbehovSerde
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
