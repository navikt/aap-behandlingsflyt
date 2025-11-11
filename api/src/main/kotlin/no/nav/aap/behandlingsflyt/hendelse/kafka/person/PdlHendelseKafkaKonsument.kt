package no.nav.aap.behandlingsflyt.hendelse.kafka.person

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
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
import java.time.Duration
import javax.sql.DataSource

const val PDL_HENDELSE_TOPIC = "pdl.leesah-v1"

class PdlHendelseKafkaKonsument(
    config: KafkaConsumerConfig<String, Personhendelse>,
    pollTimeout: Duration = Duration.ofSeconds(10L),
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider,
) : KafkaKonsument<String, Personhendelse>(
    consumerName = "PdlHendelse",
    topic = PDL_HENDELSE_TOPIC,
    config = config,
    pollTimeout = pollTimeout,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLog")
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
            val sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider)
            if (personHendelse.opplysningstype == Opplysningstype.DOEDSFALL_V1 && personHendelse.endringstype == Endringstype.OPPRETTET) {
                log.info("Håndterer hendelse med ${personHendelse.opplysningstype} og ${personHendelse.endringstype}")

                var person: Person? = null
                var funnetIdent: Ident? = null

                for (ident in personHendelse.personidenter) {
                    secureLogger.info("Håndterer hendelse for ${ident}")
                    person = personRepository.finn(Ident(ident))
                    //Håndterer D-nummer og Fnr
                    if (person != null) {
                        funnetIdent = Ident(ident)
                        break
                    }

                }

                person?.let { personIKelvin ->
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
                            sisteOpprettedeBehandling,
                            Dødsfalltype.DODSFALL_BRUKER
                        )
                    }
                }
                
                val behandlingIds = barnRepository.hentBehandlingIdForSakSomFårBarnetilleggForBarn(funnetIdent!!)
                log.info("Sjekker mottatt hendelse for barn $behandlingIds")
                behandlingIds
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
                            sisteOpprettedeBehandling,
                            Dødsfalltype.DODSFALL_BARN
                        )
                    }


            } else {
                log.info("Ignorerer hendelse med ${personHendelse.opplysningstype} og ${personHendelse.endringstype}")
            }
        }
    }

    private fun sendDødsHendelseHvisRelevant(
        behandlingMedSistFattedeVedtak: BehandlingMedVedtak?,
        underveisRepository: UnderveisRepository,
        personHendelse: PdlPersonHendelse,
        sak: Sak,
        hendelseService: MottattHendelseService,
        sisteOpprettedeBehandling: Behandling?,
        hendelseType: Dødsfalltype
    ) {
        val vedtakBehandling = behandlingMedSistFattedeVedtak
        val underveisGrunnlag = vedtakBehandling?.let { underveisRepository.hentHvisEksisterer(it.id) }

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
                            personHendelse.tilInnsendingDødsfallBruker(sak.saksnummer)
                        )
                    }

                    Dødsfalltype.DODSFALL_BARN -> {
                        log.info("Registrerer mottatt hendelse fordi dødsfall på barn. Bruker har iverksatte vedtak der minst en fremtidig periode er oppfylt ${sak.saksnummer}")
                        hendelseService.registrerMottattHendelse(
                            personHendelse.tilInnsendingDødsfallBarn(sak.saksnummer)
                        )
                    }
                }
            }
        } else if (sisteOpprettedeBehandling != null) {
            when (hendelseType) {
                Dødsfalltype.DODSFALL_BRUKER -> {
                    log.info("Registrerer mottatt hendelse fordi dødsfall på bruker. Bruker har ingen iverksatte vedtak ${sak.saksnummer}")
                    hendelseService.registrerMottattHendelse(
                        personHendelse.tilInnsendingDødsfallBruker(sak.saksnummer)
                    )
                }

                Dødsfalltype.DODSFALL_BARN -> {
                    log.info("Registrerer mottatt hendelse fordi dødsfall på barn. Bruker har ingen iverksatte vedtak ${sak.saksnummer}")
                    hendelseService.registrerMottattHendelse(
                        personHendelse.tilInnsendingDødsfallBarn(sak.saksnummer)
                    )
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