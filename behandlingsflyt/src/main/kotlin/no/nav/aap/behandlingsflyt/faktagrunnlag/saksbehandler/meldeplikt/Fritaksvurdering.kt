package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate
import java.time.LocalDateTime

data class Fritaksvurdering(
    val harFritak: Boolean,
    val fraDato: LocalDate,
    val tilDato: LocalDate? = null,
    val begrunnelse: String,
    val vurdertAv: String,
    val opprettetTid: LocalDateTime,
    val vurdertIBehandling: BehandlingId,
) {
    fun toFritaksvurderingData(): FritaksvurderingData {
        return FritaksvurderingData(
            harFritak = harFritak,
            begrunnelse = begrunnelse,
            vurdertAv = vurdertAv,
            opprettetTid = opprettetTid,
            vurdertIBehandling = vurdertIBehandling,
        )
    }

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
                a.fraDato == b.fraDato &&
                a.tilDato == b.tilDato &&
                a.begrunnelse == b.begrunnelse
    }
}