package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.GUnit
import java.time.Instant

data class BarnepensjonGrunnlag(
    val vurdering: BarnepensjonVurdering
)

data class BarnepensjonVurdering(
    val begrunnelse: String,
    val perioder: Set<BarnepensjonPeriode>,
    val vurdertIBehandling: BehandlingId,
    val vurdertAv: Bruker,
    val opprettet: Instant,
) {
    init {
        if (Periode.overlapper(perioder.map { it.periode }.toSet())) {
            throw IllegalArgumentException("Fant overlappende perioder")
        }
    }
}

data class BarnepensjonPeriode(
    val periode: Periode,
    val grunnbeløp: GUnit,
)
