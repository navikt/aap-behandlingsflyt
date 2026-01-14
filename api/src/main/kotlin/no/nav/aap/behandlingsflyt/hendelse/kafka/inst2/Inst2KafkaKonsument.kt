package no.nav.aap.behandlingsflyt.hendelse.kafka.inst2

import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Inst2HendelseKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val INSTITUSJONSOPPHOLD_EVENT_TOPIC = "team-rocket.institusjon-opphold-hendelser"

class Inst2KafkaKonsument(
    config: KafkaConsumerConfig<String, String>,
    pollTimeout: Duration = 10.seconds,
    closeTimeout: Duration = 30.seconds,
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider,
) : KafkaKonsument<String, String>(
    topic = INSTITUSJONSOPPHOLD_EVENT_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
    closeTimeout = closeTimeout,
    consumerName = "AapBehandlingsflytInstitusjonsOppholdHendelse",
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("team-logs")

    override fun håndter(meldinger: ConsumerRecords<String, String>) {
        meldinger.forEach(::håndter)
    }

    fun håndter(melding: ConsumerRecord<String, String>) {
        log.info(
            "Behandler institusjonsopphold-record med id: ${melding.key()}, partition ${melding.partition()}, offset: ${melding.offset()}"
        )
        melding.topic()
        val meldingKey = "${melding.partition()}-${melding.offset()}"
        håndter(meldingKey, melding.value())
    }

    fun håndter(meldingKey: String, meldingVerdi: String) {
        val institusjonsoppholdHendelse = try {
            DefaultJsonMapper.fromJson<Inst2HendelseKafkaMelding>(meldingVerdi)
        } catch (exception: Exception) {
            secureLogger.error("Kunne ikke parse melding fra institusjonsopphold: $meldingKey med verdi: $meldingVerdi", exception)
            throw exception
        }
        val saksnummer =
           finnSaksNummer()
        log.info("Mottatt institusjonsoppholdhendelse for saksnummer: $saksnummer")
        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val hendelseService =
                MottattHendelseService(repositoryProvider)

            //TODO: Finn saksnummer først
            /*hendelseService.registrerMottattHendelse(dto = institusjonsoppholdHendelse.tilInnsending(meldingKey,
                Saksnummer(saksnummer))
            )*/
        }

    }

    private fun finnSaksNummer(): String {
        //TODO: Finn saksnummer
        return "FAKE"
    }



}
