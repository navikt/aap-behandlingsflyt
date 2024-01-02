package no.nav.aap.behandlingsflyt.avklaringsbehov

import java.time.LocalDateTime

class Endring(val status: Status,
              val tidsstempel: LocalDateTime = LocalDateTime.now(),
              val begrunnelse: String,
              val endretAv: String) : Comparable<Endring> {

    override fun compareTo(other: Endring): Int {
        return tidsstempel.compareTo(other.tidsstempel)
    }
}
