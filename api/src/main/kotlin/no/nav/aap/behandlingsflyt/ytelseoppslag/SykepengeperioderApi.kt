package no.nav.aap.behandlingsflyt.ytelseoppslag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SykepengeoppslagService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import javax.sql.DataSource

const val SYKEPENGEPERIODER_PATH: String = "/api/person/sykepengeperioder"

fun NormalOpenAPIRoute.sykepengeperioderApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route(SYKEPENGEPERIODER_PATH) {
        authorizedPost<Unit, List<SykepengeperiodeDTO>, YtelseoppslagRequest>(
            AuthorizationBodyPathConfig(operasjon = Operasjon.SE)
        ) { _, request ->
            val perioder = dataSource.transaction(readOnly = true) { connection ->
                SykepengeoppslagService(repositoryRegistry.provider(connection), gatewayProvider)
                    .hentSykepengeperioder(request.personident, request.tilOppslagsperiode())
            }
            respond(perioder.map { it.tilDto() }, HttpStatusCode.OK)
        }
    }
}

private fun UtbetaltePerioder.tilDto() = SykepengeperiodeDTO(
    fom = fom,
    tom = tom,
    grad = grad.tilBigDecimal(),
    organisasjonsnummer = organisasjonsnummer,
)
