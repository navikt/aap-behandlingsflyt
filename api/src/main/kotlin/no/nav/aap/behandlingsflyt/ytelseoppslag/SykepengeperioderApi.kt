package no.nav.aap.behandlingsflyt.ytelseoppslag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SykepengeoppslagService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import java.math.BigDecimal
import java.math.BigInteger

const val SYKEPENGEPERIODER_PATH: String = "/api/person/sykepengeperioder"

fun NormalOpenAPIRoute.sykepengeperioderApi(
    gatewayProvider: GatewayProvider,
) {
    route(SYKEPENGEPERIODER_PATH) {
        authorizedPost<Unit, List<SykepengeperiodeDTO>, YtelseoppslagRequest>(
            AuthorizationBodyPathConfig(operasjon = Operasjon.SE)
        ) { _, request ->
            val identer = gatewayProvider.provide<IdentGateway>()
                .hentAlleIdenterForPerson(Ident(request.personident))
                .map(Ident::identifikator)
                .toSet()
                .ifEmpty { setOf(request.personident) }

            val perioder = SykepengeoppslagService(gatewayProvider)
                .hentSykepengeperioder(identer, request.tilOppslagsperiode())
            respond(perioder.map { it.tilDto() }, HttpStatusCode.OK)
        }
    }
}

internal fun Number.tilBigDecimal(): BigDecimal = when (this) {
    is BigDecimal -> this
    is BigInteger -> BigDecimal(this)
    else -> BigDecimal(this.toString())
}

private fun UtbetaltePerioder.tilDto() = SykepengeperiodeDTO(
    fom = fom,
    tom = tom,
    grad = grad.tilBigDecimal(),
    organisasjonsnummer = organisasjonsnummer,
)
