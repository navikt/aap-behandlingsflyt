package no.nav.aap.behandlingsflyt.kontrakt.ytelseoppslag

import java.math.BigDecimal
import java.time.LocalDate

public data class ForeldrepengeperioderDTO(
    val oppslagsperiode: YtelseoppslagPeriodeDTO,
    val perioder: List<ForeldrepengeperiodeDTO>,
)

public data class ForeldrepengeperiodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
    val utbetalingsgrad: BigDecimal,
    val beløp: BigDecimal?,
    val saksnummer: String?,
    val kildesystem: String,
    val ytelseStatus: String,
    val vedtattTidspunkt: LocalDate,
)

