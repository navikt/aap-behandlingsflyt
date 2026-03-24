package no.nav.aap.behandlingsflyt.behandling.meldekort

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.HentSakDTO
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForSakResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldekortApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
    clock: Clock = Clock.systemDefaultZone()
) {
    route("api/meldekort/{saksnummer}") {
        authorizedGet<HentSakDTO, MeldeperioderMedMeldekortResponse>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                sakPathParam = SakPathParam("saksnummer")
            ),
            null,
            modules = arrayOf(TagModule(listOf(Tags.Sak))),
        ) { req ->
            val meldeperioderMedMeldekortResponse = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val meldekortRepository = repositoryProvider.provide<MeldekortRepository>()
                val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val behandlingService = BehandlingService(repositoryProvider, gatewayProvider)

                val sak = sakRepository.hent(Saksnummer(req.saksnummer))
                val sisteFattedeVedtaksBehandling = behandlingService.finnBehandlingMedSisteFattedeVedtak(sak.id)

                sisteFattedeVedtaksBehandling?.let { behandling ->
                    val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id) ?: return@let null
                    val meldeperioder = hentAktuelleMeldeperioder(underveisGrunnlag, clock)
                    val meldekortene = meldekortRepository.hentHvisEksisterer(behandling.id)?.meldekort().orEmpty()

                    meldeperioder.map { meldeperiode ->
                        val meldekort = nyesteMeldekortForMeldeperiode(meldekortene, meldeperiode)

                        MeldeperiodeMedMeldekortDto(
                            meldeperiode = meldeperiode,
                            meldekort = meldekort?.toDto()
                        )
                    }
                }?.let { MeldeperioderMedMeldekortResponse(it.toSet()) }
            }

            respond(meldeperioderMedMeldekortResponse ?: MeldeperioderMedMeldekortResponse(emptySet()))
        }
    }
}

/**
 * Henter ut nyeste meldekort som sammenfaller med meldeperiode basert på innmeldte timer
 */
private fun nyesteMeldekortForMeldeperiode(
    meldekortene: List<Meldekort>,
    meldeperiode: Periode
): Meldekort? = meldekortene.lastOrNull { meldekort ->
    val arbeidsperiode = meldekort.arbeidsperiode()
    arbeidsperiode != null && meldeperiode.inneholder(arbeidsperiode)
}

/**
 * Henter meldeperioder som er aktuelle å endre for saksbehandler.
 * - Perioden må ha rettighetstype
 * - Meldeperioder bakover i tid skal inkluderes
 * - Inneværende periode skal inkluderes
 *
 * Verdt å merke seg at dersom meldeplikten ikke er oppfylt for en periode, så vil Utfall == IKKE_OPPFYLT, mens
 * rettighetstypen vil fortsatt være satt. Dermed kan saksbehandler kunne sette timer i meldekortet for perioden.
 */
private fun hentAktuelleMeldeperioder(
    underveisGrunnlag: UnderveisGrunnlag,
    clock: Clock
): List<Periode> {
    val meldeperioder = underveisGrunnlag.perioder
        .filter { it.rettighetsType != null }
        .map { it.meldePeriode }
        .sortedBy { it.fom }
        .takeWhile { it.fom < LocalDate.now(clock) }

    return meldeperioder
}

private fun Meldekort.toDto(): MeldekortDto = MeldekortDto(
    id = journalpostId.identifikator,
    mottattTidspunkt = mottattTidspunkt,
    dager = timerArbeidPerPeriode.map { arbeid ->
        DagDto(
            dato = arbeid.periode.fom,
            timerArbeidet = arbeid.timerArbeid.antallTimer.toDouble()
        )
    }.toSet()
)

data class MeldeperioderMedMeldekortResponse(
    val meldeperioderMedMeldekort: Set<MeldeperiodeMedMeldekortDto>,
)

data class MeldeperiodeMedMeldekortDto(
    val meldeperiode: Periode,
    val meldekort: MeldekortDto?
)

data class MeldekortDto(
    val id: String,
    val mottattTidspunkt: LocalDateTime,
    val dager: Set<DagDto>,
)

data class DagDto(
    val dato: LocalDate,
    val timerArbeidet: Double
)
