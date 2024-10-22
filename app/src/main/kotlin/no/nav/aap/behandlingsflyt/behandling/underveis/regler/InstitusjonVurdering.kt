package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.verdityper.Prosent

data class InstitusjonVurdering (
    val skalReduseres: Boolean = true,
    val årsak: Årsak?,
    val grad: Prosent
)

enum class Årsak {
    BARNETILLEGG,
    FORSØRGER_ELLER_HAR_FASTEKOSTNADER,
    UTEN_REDUKSJON_TRE_MND,
    UTEN_REDUKSJON_RESTERENDE_MND,
    KOST_OG_LOSJI,
    FASTE_KOSTNADER,
}