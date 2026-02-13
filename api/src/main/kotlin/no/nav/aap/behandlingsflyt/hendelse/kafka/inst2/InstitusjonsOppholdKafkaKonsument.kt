package no.nav.aap.behandlingsflyt.hendelse.kafka.inst2

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGateway
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Inst2KafkaDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.InstitusjonsOppholdHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val INSTITUSJONSOPPHOLD_EVENT_TOPIC: String =
    requiredConfigForKey("integrasjon.institusjonsopphold.event.topic")

class InstitusjonsOppholdKafkaKonsument(
    config: KafkaConsumerConfig<String, InstitusjonsOppholdHendelseKafkaMelding>,
    pollTimeout: Duration = 10.seconds,
    closeTimeout: Duration = 30.seconds,
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    val institusjonsoppholdKlient: InstitusjonsoppholdGateway,
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
            "Behandler institusjonsopphold-record med id: ${melding.key()}, partition ${melding.partition()}, offset: ${melding.offset()}, topic: $topic"
        )
        val meldingKey = "${melding.partition()}-${melding.offset()}"
        håndter(meldingKey, melding.value())
    }

    fun håndter(meldingKey: String, meldingVerdi: InstitusjonsOppholdHendelseKafkaMelding) {
        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val sakRepository: SakRepository = repositoryProvider.provide()
            val personRepository: PersonRepository = repositoryProvider.provide()
            val hendelseService =
                MottattHendelseService(repositoryProvider)
            val person = personRepository.finn(Ident(meldingVerdi.norskident))
            secureLogger.info("Prøver å finne person for ${meldingVerdi.norskident} $person")
            if (person != null) {

                val saker = sakRepository.finnSakerFor(person)

                for (saken in saker) {
                    val institusjonsopphold = institusjonsoppholdKlient.hentDataForHendelse(meldingVerdi.oppholdId)
                    val beriketInstitusjonsopphold = Inst2KafkaDto(
                        startdato = institusjonsopphold.startdato,
                        sluttdato = institusjonsopphold.sluttdato,
                    )
                    meldingVerdi.institusjonsOpphold = beriketInstitusjonsopphold
                    val sluttdato = meldingVerdi.institusjonsOpphold?.sluttdato
                    val varighetOverTreeMaaneder = institusjonsopphold.startdato
                        .withDayOfMonth(1)
                        .plusMonths(4)
                    if (sluttdato == null || sluttdato > varighetOverTreeMaaneder) {
                        hendelseService.registrerMottattHendelse(
                            dto = meldingVerdi.tilInnsending(
                                meldingKey,
                                saken.saksnummer
                            )
                        )
                        log.info("Sendt institusjonsoppholdhendelse for saksnummer: ${saken.saksnummer}")
                    } else {
                        log.info(
                            "Ignorerer institusjonsoppholdhendelse for saksnummer: ${saken.saksnummer}, " +
                                    "institusjonsoppholdet er for lenge til skal avsluttes"
                        )
                    }

                    log.info("Mottatt institusjonsoppholdhendelse for saksnummer: ${saken.saksnummer}")
                }
            }
        }

    }

}
