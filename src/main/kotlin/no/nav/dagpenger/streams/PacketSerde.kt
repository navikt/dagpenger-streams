package no.nav.dagpenger.streams

import java.lang.Exception
import java.nio.charset.StandardCharsets
import kotlinx.io.core.String
import no.nav.dagpenger.events.Packet
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer

class PacketSerializer : Serializer<Packet> {
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}

    override fun serialize(topic: String?, data: Packet?): ByteArray? {
        return data?.toJson()?.toByteArray(charset = StandardCharsets.UTF_8)
    }
}

class PacketDeserializer : Deserializer<Packet> {
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}

    override fun deserialize(topic: String?, data: ByteArray?): Packet? {
        return data?.let {
            try {
                Packet(String(data, charset = StandardCharsets.UTF_8))
            } catch (e: Exception) {
                throw SerializationException("Error when deserializing JSON to Packet", e)
            }
        }
    }
}
