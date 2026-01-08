package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant

data class SykestipendGrunnlag(
    val vurdering: SykestipendVurdering
)

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