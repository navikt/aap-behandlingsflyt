package no.nav.aap.behandlingsflyt.behandling.meldekort

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
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
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldekortApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
    clock: java.time.Clock = java.time.Clock.systemDefaultZone()
) {
    route("api/meldekort/{saksnummer}") {
        authorizedGet<HentSakDTO, MeldekorteneDto>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                sakPathParam = SakPathParam("saksnummer")
            ),
            null,
            modules = arrayOf(TagModule(listOf(Tags.Sak))),
        ) { req ->
            val meldekorteneDto = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val meldekortRepository = repositoryProvider.provide<MeldekortRepository>()
                val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val behandlingService = BehandlingService(
                    repositoryProvider = repositoryProvider,
                    gatewayProvider = gatewayProvider
                )

                val sak = sakRepository.hent(saksnummer = Saksnummer(req.saksnummer))
                val sisteFattedeVedtaksBehandling = behandlingService.finnBehandlingMedSisteFattedeVedtak(sak.id)

                sisteFattedeVedtaksBehandling?.let { behandling ->
                    val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id) ?: return@let null

                    // Henter meldeperioder som kan endres for saksbehandler
                    val meldeperiodene = underveisGrunnlag.perioder
                        .map { it.meldePeriode }
                        .sortedBy { it.fom }
                        .takeWhile { it.tom < LocalDate.now(clock) }

                    val meldekortListe = meldekortRepository.hentHvisEksisterer(behandling.id)?.meldekort().orEmpty()

                    meldeperiodene.map { meldeperiode ->
                        val meldekort = meldekortListe.firstOrNull { meldekort ->
                            val arbeidsperiode = meldekort.arbeidsperiode()
                            arbeidsperiode != null && meldeperiode.inneholder(arbeidsperiode)
                        }

                        MeldekortDto(
                            id = meldekort?.journalpostId?.identifikator,
                            meldeperiode = meldeperiode,
                            mottattTidspunkt = meldekort?.mottattTidspunkt,
                            dager = meldekort?.timerArbeidPerPeriode?.map { arbeid ->
                                DagDto(
                                    dato = arbeid.periode.fom,
                                    timerArbeidet = arbeid.timerArbeid.antallTimer.toDouble()
                                )
                            }.orEmpty()
                        )
                    }
                }?.let { MeldekorteneDto(it.toSet()) }
            }

            respond(meldekorteneDto ?: MeldekorteneDto(emptySet()))
        }
    }
}

data class MeldekorteneDto(
    val meldekortene: Set<MeldekortDto>
)

data class MeldekortDto(
    val id: String?,
    val meldeperiode: Periode,
    val mottattTidspunkt: LocalDateTime?,
    val dager: List<DagDto>,
)

data class DagDto(
    val dato: LocalDate,
    val timerArbeidet: Double
)
