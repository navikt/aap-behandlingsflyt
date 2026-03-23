package no.nav.aap.behandlingsflyt.behandling.meldekort

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.HentSakDTO
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForSakResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.meldekortApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
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
                val sak = repositoryProvider.provide<SakRepository>().hent(saksnummer = Saksnummer(req.saksnummer))
                val behandlingService = BehandlingService(
                    repositoryProvider = repositoryProvider,
                    gatewayProvider = gatewayProvider
                )
                val sisteFattedeVedtaksBehandling = behandlingService.finnBehandlingMedSisteFattedeVedtak(sak.id)

                sisteFattedeVedtaksBehandling?.let {
                    meldekortRepository.hentHvisEksisterer(it.id)?.tilDto()
                }
            }

            respond(meldekorteneDto ?: MeldekorteneDto(emptySet()))
        }
    }
}

private fun MeldekortGrunnlag.tilDto(): MeldekorteneDto {
    return MeldekorteneDto(
        meldekortene = meldekort().map { meldekort ->
            MeldekortDto(
                meldekortId = meldekort.journalpostId.identifikator,
                dager = meldekort.timerArbeidPerPeriode.map { arbeid ->
                    DagDto(
                        dato = arbeid.periode.fom,
                        timerArbeidet = arbeid.timerArbeid.antallTimer.toDouble()
                    )
                }
            )
        }.toSet()
    )
}

data class MeldekorteneDto(
    val meldekortene: Set<MeldekortDto>
)

data class MeldekortDto(
    val meldekortId: String,
    val dager: List<DagDto>
)

data class DagDto(
    val dato: LocalDate,
    val timerArbeidet: Double
)

