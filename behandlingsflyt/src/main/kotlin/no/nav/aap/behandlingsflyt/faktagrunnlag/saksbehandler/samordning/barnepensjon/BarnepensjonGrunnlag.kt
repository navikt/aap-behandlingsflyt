package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth

data class BarnepensjonGrunnlag(
    val vurdering: BarnepensjonVurdering
)

data class BarnepensjonVurdering(
    val begrunnelse: String,
    val perioder: Set<BarnepensjonPeriode>,
    val vurdertIBehandling: BehandlingId,
    val vurdertAv: Bruker,
    val opprettet: Instant,
)

data class BarnepensjonPeriode(
    val fom: YearMonth,
    val tom: YearMonth?,
    val månedbeløp: Beløp
) {
    init {
        if (månedbeløp.verdi < BigDecimal.ZERO) {
            throw IllegalArgumentException("Månedbeløp kan ikke være negativt")
        }
    }
}
