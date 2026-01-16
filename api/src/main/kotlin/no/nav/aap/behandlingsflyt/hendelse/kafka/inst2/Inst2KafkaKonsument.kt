package no.nav.aap.behandlingsflyt.hendelse.kafka.inst2

import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.InstitusjonsOppholdHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.config.requiredConfigForKey
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

val INSTITUSJONSOPPHOLD_EVENT_TOPIC: String =
    requiredConfigForKey("integrasjon.institusjonsopphold.event.topic")

class Inst2KafkaKonsument(
    config: KafkaConsumerConfig<String, InstitusjonsOppholdHendelseKafkaMelding>,
    pollTimeout: Duration = 10.seconds,
    closeTimeout: Duration = 30.seconds,
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider,
) : KafkaKonsument<String, InstitusjonsOppholdHendelseKafkaMelding>(
    topic = INSTITUSJONSOPPHOLD_EVENT_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
    closeTimeout = closeTimeout,
    consumerName = "AapBehandlingsflytInstitusjonsOppholdHendelse",
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("team-logs")

    override fun håndter(meldinger: ConsumerRecords<String, InstitusjonsOppholdHendelseKafkaMelding>) {
        meldinger.forEach(::håndter)
    }

    fun håndter(melding: ConsumerRecord<String, InstitusjonsOppholdHendelseKafkaMelding>) {
        log.info(
            "Behandler institusjonsopphold-record med id: ${melding.key()}, partition ${melding.partition()}, offset: ${melding.offset()}, topic: ${topic}"
        )
        val meldingKey = "${melding.partition()}-${melding.offset()}"
        håndter(meldingKey, melding.value())
    }

    fun håndter(meldingKey: String, meldingVerdi: InstitusjonsOppholdHendelseKafkaMelding) {
        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val sakRepository: SakRepository = repositoryProvider.provide()
            val personRepository: PersonRepository = repositoryProvider.provide()
            var person: Person? = null
            val hendelseService =
                MottattHendelseService(repositoryProvider)
            person = personRepository.finn(Ident(meldingVerdi.norskident))
            if (person != null) {

                val sak = sakRepository.finnSakerFor(person)
                for (saken in sak) {

                    hendelseService.registrerMottattHendelse(dto = meldingVerdi.tilInnsending(meldingKey,
                       saken.saksnummer)
                    )
                    log.info("Mottatt institusjonsoppholdhendelse for saksnummer: ${saken.saksnummer}")
                }
            }
        }

    }

    private fun finnSaksNummer(): String {

        //TODO: Finn saksnummer
        return "FAKE"
    }


}
