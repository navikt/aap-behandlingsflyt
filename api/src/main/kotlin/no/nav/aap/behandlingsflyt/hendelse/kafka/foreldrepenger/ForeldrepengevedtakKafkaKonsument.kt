package no.nav.aap.behandlingsflyt.hendelse.kafka.foreldrepenger

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ForeldrepengevedtakKafkaMelding
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
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

const val FORELDREPENGEVEDTAK_EVENT_TOPIC = "teamforeldrepenger.vedtak-ekstern"

class ForeldrepengevedtakKafkaKonsument(
    config: KafkaConsumerConfig<String, String>,
    pollTimeout: Duration = 10.seconds,
    closeTimeout: Duration = 30.seconds,
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider,
) : KafkaKonsument<String, String>(
    topic = FORELDREPENGEVEDTAK_EVENT_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
    closeTimeout = closeTimeout,
    consumerName = "AapBehandlingsflytForeldrepengevedtak",
) {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun håndter(meldinger: ConsumerRecords<String, String>) {
        meldinger.forEach(::håndter)
    }

    fun håndter(melding: ConsumerRecord<String, String>) {
        log.info(
            "Behandler foreldrepengevedtak-record med id: ${melding.key()}, partition ${melding.partition()}, offset: ${melding.offset()}, topic: $topic"
        )
        val meldingKey = "${melding.partition()}-${melding.offset()}"
        håndter(meldingKey, melding.value())
    }

    fun håndter(meldingKey: String, meldingVerdi: String) {
        dataSource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val sakRepository: SakRepository = repositoryProvider.provide()
            val personRepository: PersonRepository = repositoryProvider.provide()
            val hendelseService = MottattHendelseService(repositoryProvider)
            val trukketSøknadService = TrukketSøknadService(repositoryProvider)
            val behandlingService = BehandlingService(repositoryProvider, gatewayProvider)
            val foreldrepengevedtakMelding = DefaultJsonMapper.fromJson<ForeldrepengevedtakKafkaMelding>(meldingVerdi)
            val ident = Ident(foreldrepengevedtakMelding.personidentifikator)
            val person = personRepository.finn(ident) ?: finnPersonMedIdenterFraPdl(ident, personRepository)
            if (person != null) {
                val saker = sakRepository.finnSakerFor(person)
                for (saken in saker) {
                    val sisteYtelsesBehandling = behandlingService.finnSisteYtelsesbehandlingFor(saken.id)
                    if (sisteYtelsesBehandling == null) {
                        log.info("Det finnes ingen behandling - oppretter ikke mottatt foreldrepengehendelse for sak ${saken.saksnummer}")
                        continue
                    }
                    val søknadErTrukket = trukketSøknadService.søknadErTrukket(sisteYtelsesBehandling.id)
                    if (søknadErTrukket) {
                        log.info("Søknad er trukket - oppretter ikke mottatt foreldrepengehendelse for sak ${saken.saksnummer}")
                        continue
                    }

                    log.info("Oppretter mottatt foreldrepengehendelse for sak ${saken.saksnummer}")
                    hendelseService.registrerMottattHendelse(
                        dto = foreldrepengevedtakMelding.tilInnsending(
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
        personRepository: PersonRepository
    ): Person? {
        val pdlIdentGateway: IdentGateway = gatewayProvider.provide()
        val alleIdenter = pdlIdentGateway.hentAlleIdenterForPerson(ident)
        return personRepository.finn(alleIdenter)
    }

}
