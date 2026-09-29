package no.nav.aap.behandlingsflyt.arena

import java.time.LocalDate


data class ArenaSykdomsvurderingResponse(
    val vedtakId: Int,
    val begrunnelse: String?,
    val vilkar: List<ArenaVilkar>,
    val diagnoser: List<ArenaDiagnose>,
)

data class ArenaVilkar(
    val id: Long,
    val kode: String,
    val status: String,
    val begrunnelse: String?,
)

data class ArenaDiagnose(
    val kodeverk: String,
    val kode: String,
    val type: String,
    val opprettet: LocalDate
)