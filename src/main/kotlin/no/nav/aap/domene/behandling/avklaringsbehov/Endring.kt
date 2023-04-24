package no.nav.aap.domene.behandling.avklaringsbehov

import java.time.LocalDateTime

class Endring(var status: Status,
              var tidsstempel: LocalDateTime = LocalDateTime.now(),
              var begrunnelse: String,
              var endretAv: String) : Comparable<Endring> {

    override fun compareTo(other: Endring): Int {
        return tidsstempel.compareTo(other.tidsstempel)
    }
}
