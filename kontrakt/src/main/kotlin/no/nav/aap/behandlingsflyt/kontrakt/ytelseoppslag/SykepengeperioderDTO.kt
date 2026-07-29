package no.nav.aap.behandlingsflyt.kontrakt.ytelseoppslag

import java.math.BigDecimal
import java.time.LocalDate

public data class SykepengeperioderDTO(
    val oppslagsperiode: YtelseoppslagPeriodeDTO,
    val perioder: List<SykepengeperiodeDTO>,
)

public data class SykepengeperiodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: BigDecimal,
    val organisasjonsnummer: String?,
)

