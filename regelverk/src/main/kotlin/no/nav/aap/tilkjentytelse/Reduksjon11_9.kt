package no.nav.aap.tilkjentytelse

import java.time.LocalDate
import no.nav.aap.komponenter.verdityper.Beløp

data class Reduksjon11_9(
    val dato: LocalDate,
    val dagsats: Beløp,
) {
    init {
        require(dagsats.verdi() >= Beløp(0).verdi()) { "Dagsats for trekk pga 11-9 kan ikke være mindre enn 0" }
    }
}