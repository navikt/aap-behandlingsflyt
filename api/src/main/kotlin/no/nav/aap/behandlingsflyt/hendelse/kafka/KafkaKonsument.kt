package no.nav.aap.behandlingsflyt.hendelse.kafka

import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

abstract class KafkaKonsument(
    val topic: String,
    config: KafkaConsumerConfig,
    private val pollTimeout: Duration = Duration.ofSeconds(10L),
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val lukket: AtomicBoolean = AtomicBoolean(false)
    private val konsument = KafkaConsumer<String, String>(config.consumerProperties())

    var antallMeldinger = 0
        private set


    fun lukk() {
        lukket.set(true)
        konsument.wakeup() // Trigger en WakeupException for å avslutte polling
    }

    fun konsumer() {
        try {
            log.info("Starter konsumering av $topic")
            konsument.subscribe(listOf(topic))
            while (!lukket.get()) {
                val meldinger: ConsumerRecords<String, String> = konsument.poll(pollTimeout)
                håndter(meldinger)
                konsument.commitSync()
                antallMeldinger += meldinger.count()
            }
        } catch (e: WakeupException) {
            // Ignorerer exception hvis vi stenger ned
            if (!lukket.get()) throw e
        } finally {
            log.info("Ferdig med å lese hendelser fra $${this.javaClass.name} - lukker konsument")
            konsument.close()
        }
    }

    abstract fun håndter(meldinger: ConsumerRecords<String, String>)
}