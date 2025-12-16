package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate
import java.time.LocalDateTime

data class Fritaksvurdering(
    val harFritak: Boolean,
    val fraDato: LocalDate,
    val tilDato: LocalDate? = null,
    val begrunnelse: String,
    val vurdertAv: String,
    val opprettetTid: LocalDateTime,
    val vurdertIBehandling: BehandlingId? = null,
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
        val vurdertIBehandling: BehandlingId? = null,
    )
}