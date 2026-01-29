package no.nav.aap.behandlingsflyt.hendelse.kafka.person

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.SaksbehandlerOppgitteBarn
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Endringstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Navn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Opplysningstype
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PdlPersonHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.tilInnsendingDødsfallBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.tilInnsendingDødsfallBruker
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.utils.UtfallOppfyltUtils
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.person.pdl.leesah.Personhendelse

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val PDL_HENDELSE_TOPIC = "pdl.leesah-v1"

class PdlHendelseKafkaKonsument(
    config: KafkaConsumerConfig<String, Personhendelse>,
    pollTimeout: Duration = 10.seconds,
    closeTimeout: Duration = 30.seconds,
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider,
) : KafkaKonsument<String, Personhendelse>(
    consumerName = "PdlHendelse",
    topic = PDL_HENDELSE_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
    closeTimeout = closeTimeout,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("team-logs")
    private val utfallOppfyltUtils = UtfallOppfyltUtils()
    override fun håndter(meldinger: ConsumerRecords<String, Personhendelse>) {
        meldinger.forEach { record ->
            val personHendelse = record.value().tilDomain()
            håndter(record, personHendelse)
        }

    }

    fun håndter(melding: ConsumerRecord<String, Personhendelse>, personHendelse: PdlPersonHendelse) {
        dataSource.transaction {
            val repositoryProvider = repositoryRegistry.provider(it)
            val sakRepository: SakRepository = repositoryProvider.provide()
            val behandlingRepository: BehandlingRepository = repositoryProvider.provide()
            val personRepository: PersonRepository = repositoryProvider.provide()
            val barnRepository: BarnRepository = repositoryProvider.provide()
            val underveisRepository: UnderveisRepository = repositoryProvider.provide()
            val hendelseService = MottattHendelseService(repositoryProvider)
            val trukketSøknadService = TrukketSøknadService(repositoryProvider)
            val sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider)
            if (personHendelse.opplysningstype == Opplysningstype.DOEDSFALL_V1 && personHendelse.endringstype == Endringstype.OPPRETTET) {
                log.info("Håndterer hendelse med ${personHendelse.opplysningstype} og ${personHendelse.endringstype}")
                var person: Person? = null
                var saksbehandlersOppgitteBarn: SaksbehandlerOppgitteBarn.SaksbehandlerOppgitteBarn? = null
                var funnetIdent: Ident? = null
                for (ident in personHendelse.personidenter) {
                    person = personRepository.finn(Ident(ident))
                    saksbehandlersOppgitteBarn = barnRepository.finnSaksbehandlerOppgitteBarn(ident)
                    // Håndterer D-nummer og Fnr
                    if (person != null || saksbehandlersOppgitteBarn != null) {
                        funnetIdent = Ident(ident)
                        secureLogger.info("Håndterer hendelse for ident ${funnetIdent.identifikator} og navn ${personHendelse.navn?.etternavn} ")
                        break
                    }
                }

                // Sjekk om personen er et barn fr apersontabellen eller aap-mottaker
                håndterDødPersonSomBrukerEllerBarn(
                    person,
                    barnRepository,
                    funnetIdent,
                    behandlingRepository,
                    sakRepository,
                    sakOgBehandlingService,
                    underveisRepository,
                    trukketSøknadService,
                    personHendelse,
                    hendelseService
                )

                // Sjekk om personen er et barn oppgitt av saksbehandler
                håndterDødPersonSomEtBarnOppgittAvSaksbehandler(
                    saksbehandlersOppgitteBarn,
                    barnRepository,
                    funnetIdent,
                    behandlingRepository,
                    sakRepository,
                    sakOgBehandlingService,
                    underveisRepository,
                    trukketSøknadService,
                    personHendelse,
                    hendelseService
                )
            }


        }
    }

    private fun håndterDødPersonSomBrukerEllerBarn(
        person: Person?,
        barnRepository: BarnRepository,
        funnetIdent: Ident?,
        behandlingRepository: BehandlingRepository,
        sakRepository: SakRepository,
        sakOgBehandlingService: SakOgBehandlingService,
        underveisRepository: UnderveisRepository,
        trukketSøknadService: TrukketSøknadService,
        personHendelse: PdlPersonHendelse,
        hendelseService: MottattHendelseService
    ) {
        person?.let { personIKelvin ->
            val behandlingIdsForRegisterBarn =
                barnRepository.hentBehandlingIdForSakSomFårBarnetilleggForRegisterBarn(funnetIdent!!)
            val behandlingIdsForSøknadsBarn =
                barnRepository.hentBehandlingIdForSakSomFårBarnetilleggForSøknadsBarn(funnetIdent)
            val alleBarneBehandlingIds =
                behandlingIdsForRegisterBarn + behandlingIdsForSøknadsBarn
            log.info("Sjekker mottatt hendelse for barn $alleBarneBehandlingIds")
            if (alleBarneBehandlingIds.isNotEmpty()) {
                alleBarneBehandlingIds
                    .map { behandlingRepository.hent(it) }
                    .map { it.sakId }
                    .distinct()
                    .map { sakRepository.hent(it) }
                    .forEach { sak ->
                        val behandlingMedSistFattedeVedtak =
                            sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId = sak.id)
                        val sisteOpprettedeBehandling = behandlingRepository.finnSisteOpprettedeBehandlingFor(
                            sak.id,
                            listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
                        )
                        log.info("Registrerer mottatt hendelse på barn for ${sak.saksnummer}")
                        sendDødsHendelseHvisRelevant(
                            behandlingMedSistFattedeVedtak,
                            underveisRepository,
                            personHendelse,
                            sak,
                            hendelseService,
                            trukketSøknadService,
                            sisteOpprettedeBehandling,
                            Dødsfalltype.DODSFALL_BARN
                        )
                    }


            }

            // Finn sak på person
            sakRepository.finnSakerFor(personIKelvin).forEach { sak ->
                log.info("Registrerer mottatt hendelse på ${sak.saksnummer}")
                val sisteOpprettedeBehandling = behandlingRepository.finnSisteOpprettedeBehandlingFor(
                    sak.id,
                    listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
                )

                val behandlingMedSistFattedeVedtak =
                    sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId = sak.id)

                sendDødsHendelseHvisRelevant(
                    behandlingMedSistFattedeVedtak,
                    underveisRepository,
                    personHendelse,
                    sak,
                    hendelseService,
                    trukketSøknadService,
                    sisteOpprettedeBehandling,
                    Dødsfalltype.DODSFALL_BRUKER
                )
            }
        }
    }

    private fun håndterDødPersonSomEtBarnOppgittAvSaksbehandler(
        saksbehandlersOppgitteBarn: SaksbehandlerOppgitteBarn.SaksbehandlerOppgitteBarn?,
        barnRepository: BarnRepository,
        funnetIdent: Ident?,
        behandlingRepository: BehandlingRepository,
        sakRepository: SakRepository,
        sakOgBehandlingService: SakOgBehandlingService,
        underveisRepository: UnderveisRepository,
        trukketSøknadService: TrukketSøknadService,
        personHendelse: PdlPersonHendelse,
        hendelseService: MottattHendelseService
    ) {
        saksbehandlersOppgitteBarn?.let { barn ->
            val behandlingIdsForSaksbehandlerOppgitteBarn =
                barnRepository.hentBehandlingIdForSakSomFårBarnetilleggForSaksbehandlerOppgitteBarn(
                    funnetIdent!!
                )
            if (behandlingIdsForSaksbehandlerOppgitteBarn.isNotEmpty()) {
                behandlingIdsForSaksbehandlerOppgitteBarn
                    .map { behandlingRepository.hent(it) }
                    .map { it.sakId }
                    .distinct()
                    .map { sakRepository.hent(it) }
                    .forEach { sak ->
                        val behandlingMedSistFattedeVedtak =
                            sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId = sak.id)
                        val sisteOpprettedeBehandling =
                            behandlingRepository.finnSisteOpprettedeBehandlingFor(
                                sak.id,
                                listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
                            )
                        log.info("Registrerer mottatt hendelse på barn oppgitt av saksbehandler for ${sak.saksnummer}")
                        sendDødsHendelseHvisRelevant(
                            behandlingMedSistFattedeVedtak,
                            underveisRepository,
                            personHendelse,
                            sak,
                            hendelseService,
                            trukketSøknadService,
                            sisteOpprettedeBehandling,
                            Dødsfalltype.DODSFALL_BARN
                        )
                    }

            }
        }
    }

    private fun sendDødsHendelseHvisRelevant(
        behandlingMedSistFattedeVedtak: BehandlingMedVedtak?,
        underveisRepository: UnderveisRepository,
        personHendelse: PdlPersonHendelse,
        sak: Sak,
        hendelseService: MottattHendelseService,
        trukketSøknadService: TrukketSøknadService,
        sisteOpprettedeBehandling: Behandling?,
        hendelseType: Dødsfalltype
    ) {
        val vedtakBehandling = behandlingMedSistFattedeVedtak
        val underveisGrunnlag = vedtakBehandling?.let { underveisRepository.hentHvisEksisterer(it.id) }

        if (sisteOpprettedeBehandling != null && trukketSøknadService.søknadErTrukket(sisteOpprettedeBehandling.id)) {
            log.info("Ignorerer dødsfallhendelse fordi søknaden er trukket ${sak.saksnummer}")
        } else {
            if (underveisGrunnlag != null) {
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


    fun Personhendelse.tilDomain(): PdlPersonHendelse =
        PdlPersonHendelse(
            hendelseId = this.hendelseId,
            personidenter = this.personidenter,
            master = this.master,
            opprettet = this.opprettet,
            opplysningstype = try {
                Opplysningstype.valueOf(this.opplysningstype)
            } catch (e: IllegalArgumentException) {
                log.info("Fant ukjent opplysningstype fra PDL: ${this.opplysningstype}")
                Opplysningstype.UNKNOWN
            },
            endringstype = Endringstype.valueOf(this.endringstype.toString()),
            tidligereHendelseId = this.tidligereHendelseId,
            navn = this.navn?.let {
                Navn(
                    fornavn = it.fornavn,
                    etternavn = it.etternavn,
                    mellomnavn = it.mellomnavn,
                )
            }
        )

    enum class Dødsfalltype(val verdi: String) {
        DODSFALL_BRUKER("DODSFALL_BRUKER"), DODSFALL_BARN("DODSFALL_BARN")
    }

}