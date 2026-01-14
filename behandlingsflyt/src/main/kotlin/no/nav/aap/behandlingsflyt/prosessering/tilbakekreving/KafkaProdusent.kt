package no.nav.aap.behandlingsflyt.prosessering.tilbakekreving

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

abstract class KafkaProdusent<K, V>(
    val topic: String,
    config: KafkaProducerConfig<K, V>,
    producerName: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val produsent = KafkaProducer<K, V>(config.producerProperties(producerName = producerName))

    fun produser(key: K, value: V) {
        val record = ProducerRecord(topic, key, value)
        val future = produsent.send(record)
        val metadata = future.get()
        log.info("Metadata fra produsert melding: $metadata")
    }

}