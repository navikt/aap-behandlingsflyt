package no.nav.aap.behandlingsflyt.prosessering


import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BehandlingOgMeldekortService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BehandlingOgMeldekortServiceImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.time.LocalDateTime


// midlertidig klasse, skal ta i bruk datatyper fra kontrakt når de er klare
data class DetaljertMeldekortInfo(
    val person: Person,
    val saksnummer: Saksnummer,
    val meldePeriode: Periode?,
    val mottattTidspunkt: LocalDateTime,
    val timerArbeidPerPeriode: Set<ArbeidIPeriode>,
    val årsakTilOpprettelse: ÅrsakTilOpprettelse?,
    val vurderingsBehov: VurderingsbehovMedPeriode?,
    val meldepliktStatus: MeldepliktStatus?
)

class MeldekortTilApiInternUtfører(
    private val behandlingOgMeldekortService: BehandlingOgMeldekortService,
    private val saksRepository: SakRepository,
    private val underveisRepository: UnderveisRepository,
    private val apiInternGateway: ApiInternGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val behandlingId = BehandlingId(input.behandlingId())

        val sak = saksRepository.hent(sakId)
        val person = sak.person

        val meldekortOgBehandling = behandlingOgMeldekortService.hentAlle(sak)
        // TODO filter på behandlingId hvis det passer for Jobben?
        meldekortOgBehandling.forEach { (behandling, meldekortListe) ->
            val underveisGrunnlag = underveisRepository.hent(behandling.id)

            val detaljertMeldekortInfoListe = meldekortListe.map { meldekort ->
                // Underveisperiode har mye informasjon som kan være nyttig å sende med
                val underveisPeriode = finnUnderveisperiodeHvisEksisterer(meldekort, underveisGrunnlag.perioder)
                val meldePeriode = underveisPeriode?.meldePeriode
                val meldepliktStatus = underveisPeriode?.meldepliktStatus

                // Vi kan også sjekke for fritak fra meldeplikt, og rimelig grunn for å ikke oppfylle meldeplikt

                val vurderingsBehovForPerioden = behandling.vurderingsbehov().find {
                    (it.periode != null && meldePeriode?.overlapper(it.periode) == true) || it.periode == null
                }
                DetaljertMeldekortInfo(
                    person = person,
                    saksnummer = sak.saksnummer,
                    meldePeriode = meldePeriode,
                    mottattTidspunkt = meldekort.mottattTidspunkt,
                    timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode,
                    // TODO vurder om de nedenfor skal med, eller om de er unødvendige:
                    årsakTilOpprettelse = behandling.årsakTilOpprettelse,
                    vurderingsBehov = vurderingsBehovForPerioden,
                    meldepliktStatus = meldepliktStatus,
                )

            }

            try {
                detaljertMeldekortInfoListe.map {
                    apiInternGateway.sendDetaljertMeldekort(it)
                }
            } catch (e: Exception) {
                log.error(
                    "Feil ved sending av meldekort til API-intern for sak=${sakId}," + " behandling=${behandling.id}", e
                )
                throw e
            }
        }
    }

    private fun finnUnderveisperiodeHvisEksisterer(
        meldekort: Meldekort, underveisPerioder: List<Underveisperiode>
    ): Underveisperiode? {
        val arbeidsperiode = arbeidsperiodeFraMeldekort(meldekort)
        return underveisPerioder.firstOrNull {
            // alle arbeidsperiodene i meldekortet må være innenfor en og samme underveisperiode
            it.meldePeriode.inneholder(arbeidsperiode)
        }
    }

    /**
    @return Tidsperioden meldekortet inneholder arbeidstimer for.
    Merk at det kan være dager i perioden meldekortet gjelder for som det ikke er rapportert timer på.
    @param meldekort -
     **/
    private fun arbeidsperiodeFraMeldekort(meldekort: Meldekort): Periode {
        val arbeidPerioder = meldekort.timerArbeidPerPeriode.map { it.periode }
        val arbeidsPerioderStart = arbeidPerioder.minBy { it.fom }.fom
        val arbeidsPerioderSlutt = arbeidPerioder.maxBy { it.tom }.tom
        return Periode(arbeidsPerioderStart, arbeidsPerioderSlutt)
    }


    companion object : ProvidersJobbSpesifikasjon {
        override val navn = "TBD"
        override val type = "TBD"
        override val beskrivelse = """
                Push informasjon til API-intern slik at NKS kan hente den.
                """.trimIndent()

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            val sakRepository = repositoryProvider.provide<SakRepository>()

            val service = BehandlingOgMeldekortServiceImpl(
                behandlingRepository = repositoryProvider.provide<BehandlingRepository>(),
                meldekortRepository = repositoryProvider.provide<MeldekortRepository>(),
                mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>(),
                personOgSakService = PersonOgSakService(
                    gatewayProvider.provide(IdentGateway::class),
                    repositoryProvider.provide<PersonRepository>(),
                    sakRepository,
                )

            )
            return MeldekortTilApiInternUtfører(
                behandlingOgMeldekortService = service,
                apiInternGateway = gatewayProvider.provide(ApiInternGateway::class),
                saksRepository = sakRepository,
                underveisRepository = repositoryProvider.provide<UnderveisRepository>(),
            )
        }

        fun nyJobb(
            sakId: SakId,
            behandlingId: BehandlingId,
        ) = JobbInput(MeldekortTilApiInternUtfører).apply {
            forBehandling(sakId.toLong(), behandlingId.toLong())
        }
    }


}