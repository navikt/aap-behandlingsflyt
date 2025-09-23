package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import kotlin.DeprecationLevel.*

enum class UnderveisÅrsak {
    IKKE_GRUNNLEGGENDE_RETT,
    MELDEPLIKT_FRIST_IKKE_PASSERT,
    IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON,
    ARBEIDER_MER_ENN_GRENSEVERDI,
    SONER_STRAFF,
    VARIGHETSKVOTE_BRUKT_OPP,
    BRUDD_PÅ_AKTIVITETSPLIKT_11_7_STANS,
    BRUDD_PÅ_AKTIVITETSPLIKT_11_7_OPPHØR,

    @Suppress("EnumEntryName")
    @Deprecated("Ble delt i to. Brukes fremdeles i dev-db", level = ERROR)
    BRUDD_PÅ_AKTIVITETSPLIKT
}
