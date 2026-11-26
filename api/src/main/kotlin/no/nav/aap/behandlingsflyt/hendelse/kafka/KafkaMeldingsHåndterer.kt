package no.nav.aap.behandlingsflyt.hendelse.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord

interface KafkaMeldingsHåndterer<K, V> {
    val navn: String

    fun håndter(melding: ConsumerRecord<K, V>)
}
