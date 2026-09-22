package no.nav.aap.behandlingsflyt.arena

data class ArenaSykdomsvurderingRequest(
    val saksnummerArena: String,
)

data class ArenaSykdomsvurderingResponse(
    val begrunnelse: String,
    val ordinærAAP: Boolean,
    val diagnose: ArenaDiagnose,
)

data class ArenaDiagnose(
    val kodeverk: String,
    val hoveddiagnose: List<String>,
    val bidiagnose: List<String> = emptyList(),
)