package no.nav.aap.behandlingsflyt.ytelseoppslag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.ForeldrepengeUtbetaling
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.ForeldrepengeoppslagService
import no.nav.aap.behandlingsflyt.kontrakt.ytelseoppslag.ForeldrepengeperiodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.ytelseoppslag.ForeldrepengeperioderDTO
import no.nav.aap.behandlingsflyt.kontrakt.ytelseoppslag.YtelseoppslagRequest
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.token
import javax.sql.DataSource

const val FORELDREPENGEPERIODER_PATH: String = "/api/person/foreldrepengeperioder"

/**
 * Henter utbetalte foreldrepengeperioder for en person innenfor oppslagsvinduet `fom`-`tom`.
 *
 * Returnerer rådata, slik at konsumenten selv kan avgjøre hva som er relevant
 * (f.eks. om personen har mottatt foreldrepenger de siste 52 ukene).
 */
fun NormalOpenAPIRoute.foreldrepengeperioderApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val tilgangGateway = gatewayProvider.provide<TilgangGateway>()

    route(FORELDREPENGEPERIODER_PATH) {
        @Suppress("UnauthorizedPost")
        post<Unit, ForeldrepengeperioderDTO, YtelseoppslagRequest> { _, request ->
            sjekkTilgangTilPerson(tilgangGateway, request.personident, token())

            val oppslagsperiode = request.tilOppslagsperiode()

            val perioder = dataSource.transaction(readOnly = true) { connection ->
                ForeldrepengeoppslagService(repositoryRegistry.provider(connection), gatewayProvider)
                    .hentForeldrepengeperioder(request.personident, oppslagsperiode)
            }

            respond(
                ForeldrepengeperioderDTO(
                    oppslagsperiode = oppslagsperiode.tilDto(),
                    perioder = perioder.map { it.tilDto() },
                ),
                HttpStatusCode.OK,
            )
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





