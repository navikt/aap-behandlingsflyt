package no.nav.aap.arbeidsopptrapping

import java.time.Instant
import java.time.LocalDate
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.misc.PeriodisertVurdering

data class ArbeidsopptrappingVurdering(
    val begrunnelse: String,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val reellMulighetTilOpptrapping: Boolean,
    val rettPaaAAPIOpptrapping: Boolean,
    val vurdertAv: Bruker,
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