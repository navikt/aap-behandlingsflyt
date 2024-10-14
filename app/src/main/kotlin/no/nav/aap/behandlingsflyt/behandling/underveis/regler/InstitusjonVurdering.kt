package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.verdityper.Prosent

data class InstitusjonVurdering (
    val skalReduseres: Boolean = true,
    val begrunnelse: String,
    val årsak: Årsak?,
    val grad: Prosent
)

enum class Årsak {
    BARNETILLEGG,
    FORSØRGER,
    UTEN_REDUKSJON_TRE_MND,
    UTEN_REDUKSJON_RESTERENDE_MND,
    KOST_OG_LOSJI,
    FASTE_KOSTNADER,
    FORMUE_UNDER_FORVALTNING,
    ARBEID_UTENFOR_ANSTALT,
    SONER_UTENFOR_FENGSEL,
    SONER_I_FENGSEL
}