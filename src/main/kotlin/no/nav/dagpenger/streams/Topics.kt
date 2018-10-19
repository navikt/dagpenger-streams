package no.nav.dagpenger.streams

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.dagpenger.events.avro.Behov
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced

private val serdeConfig = mapOf(
        AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to
                (System.getenv("KAFKA_SCHEMA_REGISTRY_URL") ?: "http://localhost:8081")
)

object Topics {
    val JOARK_EVENTS = Topic(
            "aapen-dok-journalfoering-v1",
            keySerde = Serdes.String(),
            valueSerde = configureGenericAvroSerde()
    )

    val INNGÅENDE_JOURNALPOST = Topic(
            "privat-dagpenger-journalpost-mottatt-v1",
            keySerde = Serdes.String(),
            valueSerde = configureAvroSerde<Behov>()
    )
}
private fun configureGenericAvroSerde(): GenericAvroSerde {
    return GenericAvroSerde().apply { configure(serdeConfig, false) }
}

private fun <T : SpecificRecord?> configureAvroSerde(): SpecificAvroSerde<T> {
    return SpecificAvroSerde<T>().apply { configure(serdeConfig, false) }
}

fun <K : Any, V : GenericRecord> StreamsBuilder.consumeGenericTopic(topic: Topic<K, V>): KStream<K, V> {
    return stream<K, V>(
            topic.name, Consumed.with(topic.keySerde, topic.valueSerde)
    )
}

fun <K : Any, V : SpecificRecord> StreamsBuilder.consumeTopic(topic: Topic<K, V>): KStream<K, V> {
    return stream<K, V>(
            topic.name, Consumed.with(topic.keySerde, topic.valueSerde)
    )
}

fun <K, V> KStream<K, V>.toTopic(topic: Topic<K, V>) {
    return to(topic.name, Produced.with(topic.keySerde, topic.valueSerde))
}
