package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate

import java.time.LocalDateTime

class Historikk(val aksjon: Aksjon, val tidspunkt: LocalDateTime, val avIdent: String) : Comparable<Historikk> {
    override fun compareTo(other: Historikk): Int {
        return tidspunkt.compareTo(other.tidspunkt)
    }

    override fun toString(): String {
        return "Historikk(aksjon=$aksjon, tidspunkt=$tidspunkt, avIdent='$avIdent')"
    }
}
