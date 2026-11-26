package no.nav.aap.behandlingsflyt.hendelse.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class KafkaKonsument<K, V>(
    val topic: String,
    private val konsument: Consumer<K, V>,
    private val pollTimeout: Duration = 10.seconds,
    private val closeTimeout: Duration = 30.seconds,
    private val håndter: KafkaMeldingsHåndterer<K, V>
) {
    constructor(
        topic: String,
        config: KafkaConsumerConfig,
        pollTimeout: Duration = 10.seconds,
        closeTimeout: Duration = 30.seconds,
        håndter: KafkaMeldingsHåndterer<K, V>
    ) : this(
        topic = topic,
        konsument = KafkaConsumer<K, V>(config.consumerProperties(consumerName = håndter.navn)),
        pollTimeout = pollTimeout,
        closeTimeout = closeTimeout,
        håndter = håndter
    )

    private val log = LoggerFactory.getLogger(javaClass)
    private val lukket: AtomicBoolean = AtomicBoolean(false)

    @Volatile
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
                meldinger.forEach(håndter::håndter)
                konsument.commitSync()
                antallMeldinger += meldinger.count()
            }
        } catch (e: WakeupException) {
            // Ignorerer exception hvis vi stenger ned
            log.info("Konsument av $topic ble lukket med WakeupException")
            if (!lukket.get()) throw e
        } catch (e: Exception) {
            log.error("Feil ved innlesing av $topic", e)
        } finally {
            log.info("Ferdig med å lese hendelser fra $${this.javaClass.name} - lukker konsument")
            try {
                konsument.close(closeTimeout.toJavaDuration())
                lukket.set(true)
            } catch (e: Exception) {
                log.error("Feil ved lukking av konsument", e)
            }
        }
    }
    
    fun erLukket(): Boolean {
        return lukket.get()
    }

}
