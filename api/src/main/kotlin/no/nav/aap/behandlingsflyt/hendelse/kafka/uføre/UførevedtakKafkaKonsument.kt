package no.nav.aap.behandlingsflyt.hendelse.kafka.`uføre`

import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakKafkaMelding
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val UFØRE_VEDTAK_TOPIC = requiredConfigForKey("integrasjon.ufore.vedtak.topic")

class UførevedtakKafkaKonsument(
    config: KafkaConsumerConfig<String, String>,
    pollTimeout: Duration = 10.seconds,
    closeTimeout: Duration = 30.seconds,
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider,
) : KafkaKonsument<String, String>(
    consumerName = "AapBehandlingsflytUføreVedtak",
    topic = UFØRE_VEDTAK_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
    closeTimeout = closeTimeout,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun håndter(meldinger: ConsumerRecords<String, String>) {
        meldinger.forEach { melding ->
            log.info(
                "Behandler uførevedtak-record med id: ${melding.key()}, partition ${melding.partition()}, offset: ${melding.offset()}, topic: $topic"
            )
            val meldingKey = "${melding.partition()}-${melding.offset()}"
            håndter(meldingKey, melding.value())
        }

    }

    fun håndter(meldingKey: String, meldingVerdi: String) {
        val uførevedtakMelding = DefaultJsonMapper.fromJson<UførevedtakKafkaMelding>(meldingVerdi)
        dataSource.transaction {
            val repositoryProvider = repositoryRegistry.provider(it)
            val sakRepository: SakRepository = repositoryProvider.provide()
            val personRepository: PersonRepository = repositoryProvider.provide()
            val hendelseService = MottattHendelseService(repositoryProvider)
            val ident = Ident(uførevedtakMelding.personId)
            val person = personRepository.finn(ident) ?: finnPersonMedIdenterFraPdl(ident, personRepository)
            if (person != null) {
                val saker = sakRepository.finnSakerFor(person)
                for (saken in saker) {
                    log.info("Oppretter mottatt uførevedtakshendelse for sak ${saken.saksnummer}")
                    hendelseService.registrerMottattHendelse(
                        dto = uførevedtakMelding.tilInnsending(
                            meldingKey,
                            saken.saksnummer
                        )
                    )
                }
            } else {
                log.info("Hopper over hendelse fordi person ikke finnes i Kelvin")
            }
        }
    }

    private fun finnPersonMedIdenterFraPdl(
        ident: Ident,
        personRepository: PersonRepository,
    ): Person? {
        val pdlIdentGateway: IdentGateway = gatewayProvider.provide()
        val alleIdenter = pdlIdentGateway.hentAlleIdenterForPerson(ident)
        return personRepository.finn(alleIdenter)
    }
}
