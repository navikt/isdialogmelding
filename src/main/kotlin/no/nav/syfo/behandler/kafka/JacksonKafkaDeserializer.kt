package no.nav.syfo.behandler.kafka

import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.common.serialization.Deserializer

val mapper = configuredJacksonMapper()

class JacksonKafkaDeserializer : Deserializer<Any> {
    override fun deserialize(topic: String, data: ByteArray): String = mapper.readValue(data, String::class.java)
    override fun close() {}
}
