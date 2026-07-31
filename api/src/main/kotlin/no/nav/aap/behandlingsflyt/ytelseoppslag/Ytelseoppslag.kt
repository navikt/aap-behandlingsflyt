package no.nav.aap.behandlingsflyt.ytelseoppslag

import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.math.BigInteger

internal fun YtelseoppslagRequest.tilOppslagsperiode(): Periode = Periode(fom, tom)

internal fun Number.tilBigDecimal(): BigDecimal = when (this) {
    is BigDecimal -> this
    is BigInteger -> BigDecimal(this)
    else -> BigDecimal(this.toString())
}
