package no.nav.aap.behandlingsflyt.hendelse.kafka

import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.toJavaDuration

abstract class KafkaKonsument<K, V>(
    val topic: String,
    config: KafkaConsumerConfig<K, V>,
    consumerName: String,
    private val pollTimeout: Duration,
    private val closeTimeout: Duration,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val lukket: AtomicBoolean = AtomicBoolean(false)
    private val konsument = KafkaConsumer<K, V>(config.consumerProperties(consumerName = consumerName))

    var antallMeldinger = 0
        private set


    fun lukk() {
        log.info("Lukker konsument av $topic")
        lukket.set(true)
        konsument.wakeup() // Trigger en WakeupException for å avslutte polling
    }

    fun konsumer() {
        try {
            log.info("Starter konsumering av $topic")
            konsument.subscribe(listOf(topic))
            while (!lukket.get()) {
                val meldinger: ConsumerRecords<K, V> = konsument.poll(pollTimeout.toJavaDuration())
                try {
                    håndter(meldinger)
                    konsument.commitSync()
                    antallMeldinger += meldinger.count()
                } catch (e: Exception) {
                    log.error("Feil ved håndtering av meldinger", e)
                }
            }
        } catch (e: WakeupException) {
            // Ignorerer exception hvis vi stenger ned
            log.info("Konsument av $topic ble lukket med WakeupException")
            if (!lukket.get()) throw e
        } catch (e: Exception) {
            log.info("Feil ved innlesing av $topic", e)
        } finally {
            log.info("Ferdig med å lese hendelser fra $${this.javaClass.name} - lukker konsument")
            try {
                konsument.close(closeTimeout.toJavaDuration())
            } catch (e: Exception) {
                log.error("Feil ved lukking av konsument", e)
            }
        }
    }

    abstract fun håndter(meldinger: ConsumerRecords<K, V>)

}