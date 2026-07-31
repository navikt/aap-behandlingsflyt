package no.nav.aap.behandlingsflyt.ytelseoppslag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.ForeldrepengeUtbetaling
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.ForeldrepengeoppslagService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import javax.sql.DataSource

const val FORELDREPENGEPERIODER_PATH: String = "/api/person/foreldrepengeperioder"

fun NormalOpenAPIRoute.foreldrepengeperioderApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route(FORELDREPENGEPERIODER_PATH) {
        authorizedPost<Unit, List<ForeldrepengeperiodeDTO>, YtelseoppslagRequest>(
            AuthorizationBodyPathConfig(operasjon = Operasjon.SE)
        ) { _, request ->
            val perioder = dataSource.transaction(readOnly = true) { connection ->
                ForeldrepengeoppslagService(repositoryRegistry.provider(connection), gatewayProvider)
                    .hentForeldrepengeperioder(request.personident, request.tilOppslagsperiode())
            }
            respond(perioder.map { it.tilDto() }, HttpStatusCode.OK)
        }
    }
}

private fun ForeldrepengeUtbetaling.tilDto() = ForeldrepengeperiodeDTO(
    fom = periode.fom,
    tom = periode.tom,
    utbetalingsgrad = utbetalingsgrad.tilBigDecimal(),
    beløp = beløp?.tilBigDecimal(),
    saksnummer = saksnummer,
    kildesystem = kildesystem,
    ytelseStatus = ytelseStatus,
    vedtattTidspunkt = vedtattTidspunkt,
)
