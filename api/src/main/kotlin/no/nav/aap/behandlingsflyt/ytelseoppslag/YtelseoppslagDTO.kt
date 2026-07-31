package no.nav.aap.behandlingsflyt.ytelseoppslag

import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse
import java.math.BigDecimal
import java.time.LocalDate

data class YtelseoppslagRequest(
    val personident: String,
    val fom: LocalDate,
    val tom: LocalDate,
) : Personreferanse {
    override fun hentPersonreferanse(): String = personident
}

data class SykepengeperiodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: BigDecimal,
    val organisasjonsnummer: String?,
)

data class ForeldrepengeperiodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalingsgrad: BigDecimal,
    val beløp: BigDecimal?,
    val saksnummer: String?,
    val kildesystem: String,
    val ytelseStatus: String,
    val vedtattTidspunkt: LocalDate,
)

