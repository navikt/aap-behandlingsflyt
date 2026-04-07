package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant

data class SykestipendGrunnlag(
    val vurdering: SykestipendVurdering

) {
    fun tilMottarSykestipendTidslinje() = vurdering.tilMottarSykestipendTidslinje()
}

data class SykestipendVurdering(
    val begrunnelse: String,
    val perioder: Set<Periode>,
    val vurdertIBehandling: BehandlingId,
    val vurdertAv: Bruker,
    val opprettet: Instant,
) {
    init {
        if (Periode.overlapper(perioder)) throw IllegalArgumentException("Fant overlappende perioder")
    }

    fun tilMottarSykestipendTidslinje(): Tidslinje<Boolean> {
        val sortertePerioder = perioder.sortedBy { it.fom }
        return sortertePerioder.somTidslinje { Periode(fom = it.fom, tom = it.tom) }
            .map { true }.komprimer()
    }
}

data class SamordningSykestipendVurderingDto(
    val begrunnelse: String,
    val perioder: Set<Periode>
) {
    fun tilVurdering(bruker: Bruker, vurdertIBehandling: BehandlingId): SykestipendVurdering {
        return SykestipendVurdering(
            begrunnelse = begrunnelse,
            perioder = perioder,
            vurdertIBehandling = vurdertIBehandling,
            vurdertAv = bruker,
            opprettet = Instant.now()
        )
    }
}