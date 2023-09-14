package no.nav.aap.domene.behandling

import no.nav.aap.flyt.StegStatus
import no.nav.aap.flyt.Tilstand
import java.time.LocalDateTime

class StegTilstand(val tidspunkt: LocalDateTime = LocalDateTime.now(),
                   val tilstand: Tilstand,
                   var aktiv: Boolean = true) : Comparable<StegTilstand> {

    fun utledNesteStegStatus(): StegStatus {
        val gjeldendeStegStatus = tilstand.status()
        return gjeldendeStegStatus.neste()
    }

    fun deaktiver() {
        this.aktiv = false
    }

    override fun compareTo(other: StegTilstand): Int {
        return tidspunkt.compareTo(other.tidspunkt)
    }

    override fun toString(): String {
        return "StegTilstand(tidspunkt=$tidspunkt, tilstand=$tilstand, aktiv=$aktiv)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StegTilstand

        if (tilstand != other.tilstand) return false
        return aktiv == other.aktiv
    }

    override fun hashCode(): Int {
        var result = tilstand.hashCode()
        result = 31 * result + aktiv.hashCode()
        return result
    }

}
