package no.nav.aap.behandlingsflyt.hendelse.kafka.inst2

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGateway
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Inst2KafkaDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.InstitusjonsOppholdHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class InstitusjonsOppholdService(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider,
    private val institusjonsoppholdKlient: InstitusjonsoppholdGateway,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("team-logs")

    fun håndter(meldingKey: String, meldingVerdi: InstitusjonsOppholdHendelseKafkaMelding) {
        val sakRepository: SakRepository = repositoryProvider.provide()
        val personRepository: PersonRepository = repositoryProvider.provide()
        val hendelseService = MottattHendelseService(repositoryProvider)
        val trukketSøknadService = TrukketSøknadService(repositoryProvider)
        val behandlingService = BehandlingService(repositoryProvider, gatewayProvider)

        val person = personRepository.finn(Ident(meldingVerdi.norskident))
        secureLogger.info("Prøver å finne person for ${meldingVerdi.norskident} $person")

        if (person != null) {
            val saker = sakRepository.finnSakerFor(person.id)
            for (saken in saker) {
                val sisteYtelsesBehandling = behandlingService.finnSisteYtelsesbehandlingFor(saken.id)
                if (sisteYtelsesBehandling != null) {
                    val søknadErTrukket = trukketSøknadService.søknadErTrukket(sisteYtelsesBehandling.id)
                    if (søknadErTrukket) {
                        log.info("Institusjonsopphold oppdateres ikke, da sak med ${saken.id} er trukket")
                        continue
                    }
                }
                val institusjonsopphold = institusjonsoppholdKlient.hentDataForHendelse(meldingVerdi.oppholdId)
                val beriketInstitusjonsopphold = Inst2KafkaDto(
                    startdato = institusjonsopphold.startdato,
                    sluttdato = institusjonsopphold.sluttdato,
                )
                meldingVerdi.institusjonsOpphold = beriketInstitusjonsopphold

                hendelseService.registrerMottattHendelse(
                    dto = meldingVerdi.tilInnsending(
                        meldingKey,
                        saken.saksnummer
                    )
                )
                log.info("Sendt institusjonsoppholdhendelse for saksnummer: ${saken.saksnummer}")
            }
        }
    }
}