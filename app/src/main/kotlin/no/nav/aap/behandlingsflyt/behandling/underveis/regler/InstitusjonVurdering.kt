package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.verdityper.Prosent

data class InstitusjonVurdering (
    val skalReduseres: Boolean = true,
    val årsak: Årsak?,
    val grad: Prosent
)

enum class Årsak {
    FORSØRGER_ELLER_HAR_FASTEKOSTNADER,
    UTEN_REDUKSJON,
    KOST_OG_LOSJI,
}