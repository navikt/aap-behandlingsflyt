package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.verdityper.Beløp
import java.time.LocalDate

data class Reduksjon11_9(
    val dato: LocalDate,
    val dagsats: Beløp,
) {
    init {
        require(dagsats.verdi() >= Beløp(0).verdi()) { "Dagsats for trekk pga 11-9 kan ikke være mindre enn 0" }
    }
}