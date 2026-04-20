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
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.HentSakDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksnummerParameter
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForSakResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldekortApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
    clock: Clock = Clock.systemDefaultZone()
) {
    route("/api/meldekort/{saksnummer}") {
        authorizedGet<SaksnummerParameter, MeldeperioderMedMeldekortResponse>(
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

    route("/api/meldekort/oppdater") {
            authorizedPost<Unit, OppdaterMeldekortResponse, OppdaterMeldekortRequest>(
                AuthorizationBodyPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                    operasjon = Operasjon.SAKSBEHANDLE,
                ),
                modules = arrayOf(TagModule(listOf(Tags.Sak))),
            ) { _, body ->
                val response = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val journalføringService = JournalføringService(gatewayProvider)

                    val sak = sakRepository.hent(Saksnummer(body.saksnummer))
                    val bruker = bruker()
                    val meldeperiode = body.meldeperiode
                    val meldekort = tilMeldekort(body, bruker)
                    val tidspunkt = Instant.now()

                    /**
                     * Journalfører meldekort, men ferdigstiller ikke journalposten. Det fører til at den plukkes
                     * opp i postmottak som ferdigstiller denne på tilsvarende måte som vanlig meldekort fra
                     * bruker. Resten av flyten er lik ellers med revurdering og fasttrack.
                     */
                    val journalpostId = journalføringService.journalfør(sak, meldeperiode, meldekort, bruker, tidspunkt)

                    // TODO lagre ned "midlertidig tilstand her slik at saksbehandler kan se at vi prosesserer endringene?

                    OppdaterMeldekortResponse(journalpostId.identifikator)
                }

                respond(response)
            }
        }
    }

private fun tilMeldekort(oppdaterMeldekortRequest: OppdaterMeldekortRequest, vurdertAv: Bruker): MeldekortV0 {
    // TODO få inn begrunnelse og opprettetAv når PR for utvidelse av Meldekort-kontrakt er merget
    return MeldekortV0(
        harDuArbeidet = true,
        timerArbeidPerPeriode = oppdaterMeldekortRequest.dager.map {
            ArbeidIPeriodeV0(
                fraOgMedDato = it.dato,
                tilOgMedDato = it.dato,
                timerArbeid = it.timerArbeidet ?: 0.0,
            )
        }
    )
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
 * Henter meldeperioder som kan være aktuelle å endre for saksbehandler. Følgende kriterier gjelder:
 * - Perioden må ha en rettighetstype
 * - Meldeperioder bakover i tid inkluderes
 * - Inneværende periode inkluderes
 * - Perioder frem i tid inkluderes ikke
 *
 * Verdt å merke seg at dersom meldeplikten ikke er oppfylt for en periode, så vil Utfall == IKKE_OPPFYLT, mens
 * rettighetstypen vil fortsatt være satt. Dermed kan saksbehandler kunne sette timer i meldekortet for perioden.
 * Utfallet vil bli 0 utbetaling dersom saksbehandler ikke gjør annet enn å føre timer, og man er forbi meldevinduet.
 *
 * For at en bruker som i utgangspunktet ikke har oppfylt meldeplikten (ikke meldt seg til NKS, ikke sendt inn meldekort)
 * skal få utbetalt, må saksbehandler sørge for at meldeplikten oppfylles, enten ved
 * - Sette mottattdato på dokumentet innenfor meldevinduet. Dette er riktig dersom saksbehandler bare har vært treig med å legge inn timene.
 * - Gi fritak, dersom bruker skulle hatt fritak.
 * - Gi rimelig grunn, dersom bruker faktisk hadde rimelig grunn til ikke å ha meldt seg.
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