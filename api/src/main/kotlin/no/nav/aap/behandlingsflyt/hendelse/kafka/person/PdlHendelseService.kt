package no.nav.aap.behandlingsflyt.hendelse.kafka.person

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.SaksbehandlerOppgitteBarn
import no.nav.aap.behandlingsflyt.hendelse.kafka.person.PdlHendelseKafkaKonsument.Dødsfalltype
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Endringstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Opplysningstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PdlPersonHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.tilInnsendingDødsfallBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.tilInnsendingDødsfallBruker
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.utils.UtfallOppfyltUtils
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class PdlHendelseService(
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val personRepository: PersonRepository,
    private val barnRepository: BarnRepository,
    private val underveisRepository: UnderveisRepository,
    private val hendelseService: MottattHendelseService,
    private val trukketSøknadService: TrukketSøknadService,
    private val behandlingService: BehandlingService,
) {
    constructor(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
    ) : this(
        sakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        personRepository = repositoryProvider.provide(),
        barnRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        hendelseService = MottattHendelseService(repositoryProvider),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
    )

    private val log = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("team-logs")
    private val utfallOppfyltUtils = UtfallOppfyltUtils()

    fun håndter(personHendelse: PdlPersonHendelse) {
        if (personHendelse.opplysningstype == Opplysningstype.DOEDSFALL_V1 && personHendelse.endringstype == Endringstype.OPPRETTET) {
            log.info("Håndterer hendelse med ${personHendelse.opplysningstype} og ${personHendelse.endringstype}")
            var person: Person? = null
            var saksbehandlersOppgitteBarn: SaksbehandlerOppgitteBarn.SaksbehandlerOppgitteBarn? = null
            var funnetIdent: Ident? = null
            for (ident in personHendelse.personidenter.map(::Ident)) {
                person = personRepository.finn(ident)
                saksbehandlersOppgitteBarn = barnRepository.finnSaksbehandlerOppgitteBarn(ident)
                // Håndterer D-nummer og Fnr
                if (person != null || saksbehandlersOppgitteBarn != null) {
                    funnetIdent = ident
                    secureLogger.info("Håndterer hendelse for ident ${funnetIdent.identifikator} og navn ${personHendelse.navn?.etternavn} ")
                    break
                }
            }

            // Sjekk om personen er et barn fr apersontabellen eller aap-mottaker
            if (person != null) {
                håndterDødPersonSomBrukerEllerBarn(
                    person,
                    funnetIdent!!,
                    personHendelse,
                )
            }

            // Sjekk om personen er et barn oppgitt av saksbehandler
            if (saksbehandlersOppgitteBarn != null) {
                håndterDødPersonSomEtBarnOppgittAvSaksbehandler(
                    funnetIdent!!,
                    personHendelse,
                )
            }
        }
    }

    private fun håndterDødPersonSomBrukerEllerBarn(
        person: Person,
        funnetIdent: Ident,
        personHendelse: PdlPersonHendelse,
    ) {
        val behandlingIdsForRegisterBarn =
            barnRepository.hentBehandlingIdForSakSomFårBarnetilleggForRegisterBarn(funnetIdent)
        val behandlingIdsForSøknadsBarn =
            barnRepository.hentBehandlingIdForSakSomFårBarnetilleggForSøknadsBarn(funnetIdent)
        val alleBarneBehandlingIds =
            behandlingIdsForRegisterBarn + behandlingIdsForSøknadsBarn
        log.info("Sjekker mottatt hendelse for barn $alleBarneBehandlingIds")
        alleBarneBehandlingIds
            .map { behandlingRepository.hent(it) }
            .map { it.sakId }
            .distinct()
            .map { sakRepository.hent(it) }
            .forEach { sak ->
                val behandlingMedSistFattedeVedtak =
                    behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId = sak.id)
                val sisteOpprettedeBehandling = behandlingRepository.finnSisteOpprettedeBehandlingFor(
                    sak.id,
                    listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
                )
                log.info("Registrerer mottatt hendelse på barn for ${sak.saksnummer}")
                sendDødsHendelseHvisRelevant(
                    behandlingMedSistFattedeVedtak,
                    personHendelse,
                    sak,
                    sisteOpprettedeBehandling,
                    Dødsfalltype.DODSFALL_BARN
                )
            }

        // Finn sak på person
        sakRepository.finnSakerFor(person).forEach { sak ->
            log.info("Registrerer mottatt hendelse på ${sak.saksnummer}")
            val sisteOpprettedeBehandling = behandlingRepository.finnSisteOpprettedeBehandlingFor(
                sak.id,
                listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
            )

            val behandlingMedSistFattedeVedtak =
                behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId = sak.id)

            sendDødsHendelseHvisRelevant(
                behandlingMedSistFattedeVedtak,
                personHendelse,
                sak,
                sisteOpprettedeBehandling,
                Dødsfalltype.DODSFALL_BRUKER
            )
        }
    }

    private fun håndterDødPersonSomEtBarnOppgittAvSaksbehandler(
        funnetIdent: Ident,
        personHendelse: PdlPersonHendelse,
    ) {
        val behandlingIdsForSaksbehandlerOppgitteBarn =
            barnRepository.hentBehandlingIdForSakSomFårBarnetilleggForSaksbehandlerOppgitteBarn(
                funnetIdent
            )
        behandlingIdsForSaksbehandlerOppgitteBarn
            .map { behandlingRepository.hent(it) }
            .map { it.sakId }
            .distinct()
            .map { sakRepository.hent(it) }
            .forEach { sak ->
                val behandlingMedSistFattedeVedtak =
                    behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId = sak.id)
                val sisteOpprettedeBehandling =
                    behandlingRepository.finnSisteOpprettedeBehandlingFor(
                        sak.id,
                        listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
                    )
                log.info("Registrerer mottatt hendelse på barn oppgitt av saksbehandler for ${sak.saksnummer}")
                sendDødsHendelseHvisRelevant(
                    behandlingMedSistFattedeVedtak,
                    personHendelse,
                    sak,
                    sisteOpprettedeBehandling,
                    Dødsfalltype.DODSFALL_BARN
                )
            }
    }

    private fun sendDødsHendelseHvisRelevant(
        vedtakBehandling: BehandlingMedVedtak?,
        personHendelse: PdlPersonHendelse,
        sak: Sak,
        sisteOpprettedeBehandling: Behandling?,
        hendelseType: Dødsfalltype
    ) {
        val underveisGrunnlag = vedtakBehandling?.let { underveisRepository.hentHvisEksisterer(it.id) }

        if (sisteOpprettedeBehandling != null && trukketSøknadService.søknadErTrukket(sisteOpprettedeBehandling.id)) {
            log.info("Ignorerer dødsfallhendelse fordi søknaden er trukket ${sak.saksnummer}")
        } else if (underveisGrunnlag != null) {
            val personHarBareAvslagFremover =
                utfallOppfyltUtils.alleEventuellePerioderEtterOpprettetTidspunktHarUtfallIkkeOppfylt(
                    opprettetTidspunkt = personHendelse.opprettet,
                    underveisGrunnlag = underveisGrunnlag
                )

            if (personHarBareAvslagFremover) {
                log.info("Ignorerer dødsfallhendelse fordi bruker har fått avslag på alle perioder fremover ${sak.saksnummer}")
            } else {
                when (hendelseType) {
                    Dødsfalltype.DODSFALL_BRUKER -> {
                        log.info("Registrerer mottatt hendelse fordi dødsfall på bruker. Bruker har iverksatte vedtak der minst en fremtidig periode er oppfylt ${sak.saksnummer}")
                        hendelseService.registrerMottattHendelse(
                            personHendelse.tilInnsendingDødsfallBruker(
                                sak.saksnummer,
                                personHendelse.navn,
                                personHendelse.personidenter
                            )
                        )
                    }

                    Dødsfalltype.DODSFALL_BARN -> {
                        log.info("Registrerer mottatt hendelse fordi dødsfall på barn. Bruker har iverksatte vedtak der minst en fremtidig periode er oppfylt ${sak.saksnummer}")
                        hendelseService.registrerMottattHendelse(
                            personHendelse.tilInnsendingDødsfallBarn(
                                sak.saksnummer,
                                personHendelse.navn,
                                personHendelse.personidenter
                            )
                        )
                    }
                }
            }
        } else if (sisteOpprettedeBehandling != null) {
            when (hendelseType) {
                Dødsfalltype.DODSFALL_BRUKER -> {
                    log.info("Registrerer mottatt hendelse fordi dødsfall på bruker. Bruker har ingen iverksatte vedtak ${sak.saksnummer}")
                    hendelseService.registrerMottattHendelse(
                        personHendelse.tilInnsendingDødsfallBruker(
                            sak.saksnummer,
                            personHendelse.navn,
                            personHendelse.personidenter
                        )
                    )
                }

                Dødsfalltype.DODSFALL_BARN -> {
                    log.info("Registrerer mottatt hendelse fordi dødsfall på barn. Bruker har ingen iverksatte vedtak ${sak.saksnummer}")
                    hendelseService.registrerMottattHendelse(
                        personHendelse.tilInnsendingDødsfallBarn(
                            sak.saksnummer,
                            personHendelse.navn,
                            personHendelse.personidenter
                        )
                    )
                }
            }
        }
    }
}