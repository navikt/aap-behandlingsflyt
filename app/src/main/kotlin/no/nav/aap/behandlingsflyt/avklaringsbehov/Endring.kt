package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.ÅrsakTilRetur
import java.time.LocalDateTime

class Endring(
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val begrunnelse: String,
    val endretAv: String,
    val årsakTilRetur: ÅrsakTilRetur? = null,
    val årsakTilReturFritekst: String? = null
) : Comparable<Endring> {

    override fun compareTo(other: Endring): Int {
        return tidsstempel.compareTo(other.tidsstempel)
    }
}
