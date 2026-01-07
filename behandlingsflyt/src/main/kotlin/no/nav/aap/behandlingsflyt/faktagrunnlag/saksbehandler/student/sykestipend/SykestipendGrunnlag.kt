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
        perioder.sortedBy { it.fom }
            .windowed(2, 1)
            .forEach { (denne, neste) ->
                if (denne.tom < denne.fom || denne.tom >= neste.fom) {
                    throw IllegalArgumentException("Perioder for sykestipendvurdering kan ikke overlappe: $denne og $neste")
                }
            }
    }
}

data class SamordningSykestipendVurderingDto(
    val begrunnelse: String
)