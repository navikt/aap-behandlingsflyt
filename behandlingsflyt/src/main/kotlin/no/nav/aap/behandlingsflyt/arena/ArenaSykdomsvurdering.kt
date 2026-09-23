package no.nav.aap.behandlingsflyt.arena

import java.time.LocalDate


data class ArenaSykdomsvurderingResponse(
    val begrunnelse: String,
    val vilkar: List<ArenaVilkar>,
    val diagnoser: List<ArenaDiagnose>,
)

data class ArenaDiagnose(
    val kodeverk: String,
    val kode: String,
    val type: ArenaDiagnoseType,
    val opprettet: LocalDate
)

enum class ArenaDiagnoseType {
    HOVEDDIAGNOSE,
    BIDIAGNOSE
}

data class ArenaVilkar(
    val kode: String,
    val oppfylt: Boolean,
)