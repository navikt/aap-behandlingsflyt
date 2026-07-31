package no.nav.aap.behandlingsflyt.ytelseoppslag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SykepengeoppslagService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.kontrakt.ytelseoppslag.SykepengeperiodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.ytelseoppslag.YtelseoppslagRequest
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.token
import javax.sql.DataSource

const val SYKEPENGEPERIODER_PATH: String = "/api/person/sykepengeperioder"

fun NormalOpenAPIRoute.sykepengeperioderApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val tilgangGateway = gatewayProvider.provide<TilgangGateway>()

    route(SYKEPENGEPERIODER_PATH) {
        @Suppress("UnauthorizedPost")
        post<Unit, List<SykepengeperiodeDTO>, YtelseoppslagRequest> { _, request ->
            sjekkTilgangTilPerson(tilgangGateway, request.personident, token())

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
