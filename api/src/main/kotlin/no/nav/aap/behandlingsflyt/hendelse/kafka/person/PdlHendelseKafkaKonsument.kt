package no.nav.aap.behandlingsflyt.hendelse.kafka.person

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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.dbconnect.transaction
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
    private val repositoryRegistry: RepositoryRegistry
) : KafkaKonsument<String, Personhendelse>(
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
        log.info(
            "Behandler hendelse fra PDL med id: {}, partition {}, offset: {}",
            melding.key(),
            melding.partition(),
            melding.offset(),
        )
        dataSource.transaction {
            val repositoryProvider = repositoryRegistry.provider(it)
            val sakRepository: SakRepository = repositoryProvider.provide()
            val behandlingRepository: BehandlingRepository = repositoryProvider.provide()
            val personRepository: PersonRepository = repositoryProvider.provide()
            val barnRepository: BarnRepository = repositoryProvider.provide()
            val underveisRepository: UnderveisRepository = repositoryProvider.provide()
            val hendelseService = MottattHendelseService(repositoryProvider)
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
                        val behandling = behandlingRepository.finnSisteOpprettedeBehandlingFor(
                            sak.id,
                            listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
                        )

                        if (behandling != null) {
                            val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id)
                            if (underveisGrunnlag != null) {
                                val personHarBareAvslagFremover = utfallOppfyltUtils.allePerioderEtterOpprettetTidspunktHarUtfallIkkeOppfylt(
                                    opprettetTidspunkt = personHendelse.opprettet,
                                    underveisGrunnlag = underveisGrunnlag
                                )
                                if (!personHarBareAvslagFremover) {
                                    log.info("Registrerer mottatt hendelse fordi dødsfall på bruker ${sak.saksnummer}")
                                    hendelseService.registrerMottattHendelse(
                                        personHendelse.tilInnsendingDødsfallBruker(sak.saksnummer)
                                    )
                                } else {
                                    log.info("Ignorerer dødsfallhendelse fordi bruker har fått avslag på aller perioder fremover ${sak.saksnummer}")
                                }
                            }
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
                            log.info("Registrerer mottatt hendelse på barn for ${sak.saksnummer}")
                            hendelseService.registrerMottattHendelse(
                                personHendelse.tilInnsendingDødsfallBarn(sak.saksnummer)
                            )
                        }

                }

            } else {
                log.info("Ignorerer hendelse med ${personHendelse.opplysningstype} og ${personHendelse.endringstype}")
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
}