package no.nav.aap.behandlingsflyt.hendelse.kafka.tilbakekreving

import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val TILBAKEKREVING_EVENT_TOPIC = "tilbake.privat-tilbakekreving-arbeidsavklaringspenger"

class TilbakekrevingKafkaKonsument(
    config: KafkaConsumerConfig<String, String>,
    pollTimeout: Duration = 10.seconds,
    closeTimeout: Duration = 30.seconds,
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
) : KafkaKonsument<String, String>(
    topic = TILBAKEKREVING_EVENT_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
    closeTimeout = closeTimeout,
    consumerName = "AapBehandlingsflytTilbakekrevingHendelse",
) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun håndter(meldinger: ConsumerRecords<String, String>) {
        meldinger.forEach(::håndter)
    }

    fun håndter(melding: ConsumerRecord<String, String>) {
        log.info(
            "Behandler tilbakekreving-record med id: {}, partition {}, offset: {}",
            melding.key(),
            melding.partition(),
            melding.offset(),
        )
        melding.topic()
        håndter(melding.key(), melding.value())
    }

    fun håndter(meldingKey: String, meldingVerdi: String) {
        val tilbakekrevingHendelse = DefaultJsonMapper.fromJson<TilbakekrevingHendelseKafkaMelding>(meldingVerdi)
        val saksnummer = Saksnummer(tilbakekrevingHendelse.eksternFagsakId)
        log.info("Mottatt tilbakekrevinghendelse for saksnummer: $saksnummer")
        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val hendelseService = MottattHendelseService(repositoryProvider)
            hendelseService.registrerMottattHendelse(dto = tilbakekrevingHendelse.tilInnsending(meldingKey, saksnummer))
        }

    }

}
