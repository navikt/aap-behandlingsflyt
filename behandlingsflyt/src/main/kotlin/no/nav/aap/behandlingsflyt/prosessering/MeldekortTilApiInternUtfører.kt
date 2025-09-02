package no.nav.aap.behandlingsflyt.prosessering


import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BehandlingOgMeldekortService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BehandlingOgMeldekortServiceImpl
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
    val meldePeriode: Periode,
    val mottattTidspunkt: LocalDateTime,
    val timerArbeidPerPeriode: Set<ArbeidIPeriode>,
    val årsakTilOpprettelse: ÅrsakTilOpprettelse?,
    val vurderingsBehov: VurderingsbehovMedPeriode?,
    val harIkkeværtAktivitetIDetSiste: Boolean
)

class MeldekortTilApiInternUtfører(
    private val behandlingOgMeldekortService: BehandlingOgMeldekortService,
    private val meldeperiodeRepository: MeldeperiodeRepository,
    private val saksRepository: SakRepository,
    private val apiInternGateway: ApiInternGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val behandlingId = BehandlingId(input.behandlingId())

        val sak = saksRepository.hent(sakId)
        val person = sak.person

        val meldekortOgBehandling = behandlingOgMeldekortService.hentAlle(sak)
        // TODO filter på behandlingId?
        meldekortOgBehandling.forEach { (behandling, meldekortListe) ->
            // meldeperioder skal alltid finnes når det finnes mottatte meldekort på en behandling
            val meldeperioder = meldeperiodeRepository.hent(behandling.id)
            require(meldeperioder.isNotEmpty(), { "Fikk ikke meldeperioder for behandling=${behandling.id}" })

            val detaljertMeldekortInfoListe = meldekortListe.map { meldekort ->
                val aktuellMeldeperiode = finnAktuellMeldePeriode(meldekort, meldeperioder)
                    ?: error("Fant ikke meldekortets meldeperiode på behandling=${behandling.id}, meldekort=${meldekort}")

                DetaljertMeldekortInfo(
                    person = person,
                    saksnummer = sak.saksnummer,
                    meldePeriode = aktuellMeldeperiode,
                    mottattTidspunkt = meldekort.mottattTidspunkt,
                    timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode,
                    // TODO vurder om de nedenfor skal med, eller om de er unødvendige:
                    årsakTilOpprettelse = behandling.årsakTilOpprettelse,
                    vurderingsBehov = behandling.vurderingsbehov()
                        .find { it.periode != null && it.periode.overlapper(aktuellMeldeperiode) },
                    harIkkeværtAktivitetIDetSiste = behandling.harIkkeVærtAktivitetIDetSiste()
                )

            }

            try {
                detaljertMeldekortInfoListe.map {
                    apiInternGateway.sendDetaljertMeldekort(it)
                }
            } catch (e: Exception) {
                log.error(
                    "Feil ved sending av meldekort til API-intern for sak=${sakId}, " +
                            "behandling=${behandling.id}", e
                )
                throw e
            }
        }
    }

    private fun finnAktuellMeldePeriode(
        meldekort: Meldekort,
        meldeperioder: List<Periode>
    ): Periode? {
        val arbeidPerioder = meldekort.timerArbeidPerPeriode.map { it.periode }
        val arbeidsPerioderStart = arbeidPerioder.minBy { it.fom }.fom
        val arbeidsPerioderSlutt = arbeidPerioder.maxBy { it.tom }.tom
        val arbeidsPeriodeFraMeldekort = Periode(arbeidsPerioderStart, arbeidsPerioderSlutt)

        val aktuellMeldeperiode =
            meldeperioder.firstOrNull { periode -> periode.inneholder(arbeidsPeriodeFraMeldekort) }

        return aktuellMeldeperiode
    }


    companion object : ProvidersJobbSpesifikasjon {
        override val navn = "TBD"
        override val type = "TBD"
        override val beskrivelse = """
                Push informasjon til API-intern slik at NKS kan hente den.
                """.trimIndent()

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            val meldeperiodeRepository = repositoryProvider.provide<MeldeperiodeRepository>()
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
                apiInternGateway = gatewayProvider.provide(
                    ApiInternGateway::class
                ), meldeperiodeRepository = meldeperiodeRepository,
                saksRepository = sakRepository
            )
        }

        fun nyJobb(
            sakId: SakId,
            behandlingId: BehandlingId,
        ) = JobbInput(MeldekortTilApiInternUtfører)
            .apply {
                forBehandling(sakId.toLong(), behandlingId.toLong())
            }
    }


}