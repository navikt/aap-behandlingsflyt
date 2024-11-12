package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.verdityper.Beløp

data class BeregningYrkeskaderBeløpVurdering(
    internal val id: Long? = null,
    val vurderinger: List<YrkesskadeBeløpVurdering>
)

data class YrkesskadeBeløpVurdering(
    val antattÅrligInntekt: Beløp,
    val referanse: String,
    val begrunnelse: String
)