package no.nav.aap.domene.behandling

import java.time.LocalDateTime

class StegTilstand(var tidspunkt: LocalDateTime = LocalDateTime.now(),
                   var tilstand: no.nav.aap.flyt.Tilstand,
                   var aktiv: Boolean = true) : Comparable<StegTilstand> {


    fun deaktiver() {
        this.aktiv = false
    }

    override fun compareTo(other: StegTilstand): Int {
        return tidspunkt.compareTo(other.tidspunkt)
    }
}
