package no.nav.aap.behandlingsflyt.behandling.beregning.tidspunkt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.komponenter.verdityper.Beløp
import java.time.LocalDate

data class BeregningYrkesskadeAvklaringDto(
    val skalVurderes: List<YrkesskadeTilVurdering>,
    val vurderinger: List<YrkesskadeBeløpVurdering>
)

data class YrkesskadeTilVurdering(val referanse: String, val skadeDato: LocalDate, val grunnbeløp: Beløp)