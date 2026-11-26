package no.nav.aap.behandlingsflyt.hendelse.kafka.sykepenger

import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaMeldingsHåndterer
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SykepengevedtakKafkaMelding
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
import org.slf4j.LoggerFactory
import javax.sql.DataSource

const val SYKEPENGEVEDTAK_EVENT_TOPIC = "tbd.boo"

class SykepengevedtakMeldingsHåndterer(
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider
) : KafkaMeldingsHåndterer<String, String> {
    override val navn = "AapBehandlingsflytSykepengevedtak"

    private val log = LoggerFactory.getLogger(javaClass)

    override fun håndter(melding: ConsumerRecord<String, String>) {
        log.info(
            "Behandler sykepengevedtak-record med id: ${melding.key()}, partition ${melding.partition()}, offset: ${melding.offset()}, topic: ${melding.topic()}"
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
            val sykepengevedtakMelding = DefaultJsonMapper.fromJson<SykepengevedtakKafkaMelding>(meldingVerdi)
            val ident = Ident(sykepengevedtakMelding.personidentifikator)
            val person = personRepository.finn(ident) ?: finnPersonMedIdenterFraPdl(ident, personRepository)
            if (person != null) {
                val saker = sakRepository.finnSakerFor(person.id)
                for (saken in saker) {
                    val sisteYtelsesBehandling = behandlingService.finnSisteGjeldendeEllerÅpneYtelsesbehandling(saken.id)
                    if (sisteYtelsesBehandling == null) {
                        log.info("Det finnes ingen behandling - oppretter ikke mottatt sykepengehendelse for sak ${saken.saksnummer}")
                        continue
                    }
                    val søknadErTrukket = trukketSøknadService.søknadErTrukket(sisteYtelsesBehandling.id)
                    if (søknadErTrukket) {
                        log.info("Søknad er trukket - oppretter ikke mottatt sykepengehendelse for sak ${saken.saksnummer}")
                        continue
                    }
                    log.info("Oppretter mottatt sykepengehendelse for sak ${saken.saksnummer}")
                    hendelseService.registrerMottattHendelse(
                        dto = sykepengevedtakMelding.tilInnsending(
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
