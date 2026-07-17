package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate

data class ArbeidsopptrappingVurdering(
    val begrunnelse: String,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val reellMulighetTilOpptrapping: Boolean,
    val rettPaaAAPIOpptrapping: Boolean,
    val vurdertAv: String,
    override val opprettet: Instant,
    override val vurdertIBehandling: BehandlingId,
) : PeriodisertVurdering

fun List<ArbeidsopptrappingVurdering>.erFunksjoneltLik(other: List<ArbeidsopptrappingVurdering>): Boolean {
    if (this.size != other.size) return false

    return this.zip(other).all { (a, b) ->
        a.begrunnelse == b.begrunnelse &&
                a.fom == b.fom &&
                a.tom == b.tom &&
                a.rettPaaAAPIOpptrapping == b.rettPaaAAPIOpptrapping &&
                a.reellMulighetTilOpptrapping == b.reellMulighetTilOpptrapping
    }
}