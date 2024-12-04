package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.flate

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class PeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate?
) {
    constructor(periode: Periode) : this(
        fom = periode.fom, tom = if (periode.tom == Tid.MAKS) null else periode.tom
    )
}