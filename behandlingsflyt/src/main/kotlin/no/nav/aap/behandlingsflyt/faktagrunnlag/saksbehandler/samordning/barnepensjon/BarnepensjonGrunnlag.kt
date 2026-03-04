package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import java.math.BigDecimal
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
        if (Periode.overlapper(perioder.map { it.periode })) {
            throw IllegalArgumentException("Fant overlappende perioder")
        }
    }
}

data class BarnepensjonPeriode(
    val periode: Periode,
    val månedbeløp: Beløp
) {
    init {
        if (månedbeløp.verdi < BigDecimal.ZERO) {
            throw IllegalArgumentException("Månedbeløp kan ikke være negativt")
        }
        if ( periode.fom.dayOfMonth != 1) {
            throw IllegalArgumentException("Periode må starte på første dag i måneden")
        }
        if (periode.tom.dayOfMonth != periode.tom.lengthOfMonth()) {
            throw IllegalArgumentException("Periode må slutte på siste dag i måneden")
        }

    }
}
