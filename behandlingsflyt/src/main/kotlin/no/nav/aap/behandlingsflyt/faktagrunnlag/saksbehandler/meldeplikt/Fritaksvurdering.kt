package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class Fritaksvurdering(
    val harFritak: Boolean,
    override val fom: LocalDate,
    override val tom: LocalDate? = null,
    val begrunnelse: String,
    val vurdertAv: String,
    val opprettetTid: LocalDateTime,
    override val vurdertIBehandling: BehandlingId,
) : PeriodisertVurdering {
    fun toFritaksvurderingData(): FritaksvurderingData {
        return FritaksvurderingData(
            harFritak = harFritak,
            begrunnelse = begrunnelse,
            vurdertAv = vurdertAv,
            opprettetTid = opprettetTid,
            vurdertIBehandling = vurdertIBehandling,
        )
    }

    override val opprettet: Instant = opprettetTid.atZone(ZoneId.of("Europe/Oslo")).toInstant()

    data class FritaksvurderingData(
        val harFritak: Boolean,
        val begrunnelse: String,
        val vurdertAv: String,
        val opprettetTid: LocalDateTime,
        val vurdertIBehandling: BehandlingId,
    )
}

fun List<Fritaksvurdering>.erFunksjoneltLik(other: List<Fritaksvurdering>): Boolean {
    if (this.size != other.size) return false
    return this.zip(other).all { (a, b) ->
        a.harFritak == b.harFritak &&
                a.fom == b.fom &&
                a.tom == b.tom &&
                a.begrunnelse == b.begrunnelse
    }
}