package no.nav.aap.behandlingsflyt.ytelseoppslag

import no.nav.aap.behandlingsflyt.kontrakt.ytelseoppslag.YtelseoppslagRequest
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.Operasjon
import java.math.BigDecimal
import java.math.BigInteger

internal fun YtelseoppslagRequest.tilOppslagsperiode(): Periode {
    require(!fom.isAfter(tom)) { "fom kan ikke være etter tom" }
    return Periode(fom, tom)
}

internal fun Number.tilBigDecimal(): BigDecimal = when (this) {
    is BigDecimal -> this
    is BigInteger -> BigDecimal(this)
    else -> BigDecimal(this.toString())
}

internal suspend fun sjekkTilgangTilPerson(
    tilgangGateway: TilgangGateway,
    personident: String,
    token: OidcToken,
) {
    if (!tilgangGateway.sjekkTilgangTilPerson(personident, token, Operasjon.SE)) {
        throw IkkeTillattException("Har ikke tilgang til person")
    }
}
