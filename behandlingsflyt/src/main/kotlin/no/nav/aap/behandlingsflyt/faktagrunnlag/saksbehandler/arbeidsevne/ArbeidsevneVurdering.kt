package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate
import java.time.LocalDateTime

data class ArbeidsevneVurdering(
    val begrunnelse: String,
    val arbeidsevne: Prosent,
    val fraDato: LocalDate,
    val tilDato: LocalDate? = null,
    val vurdertIBehandling: BehandlingId,
    val opprettetTid: LocalDateTime,
    val vurdertAv: String,
) {
    fun toArbeidsevneVurderingData(): ArbeidsevneVurderingData {
        return ArbeidsevneVurderingData(
            begrunnelse = begrunnelse,
            arbeidsevne = arbeidsevne,
            opprettetTid = opprettetTid,
            vurdertAv = vurdertAv,
            vurdertIBehandling = vurdertIBehandling
        )
    }

    data class ArbeidsevneVurderingData(
        val begrunnelse: String,
        val arbeidsevne: Prosent,
        val opprettetTid: LocalDateTime,
        val vurdertAv: String,
        val vurdertIBehandling: BehandlingId,
    )
}
