package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.aap.komponenter.verdityper.Beløp

data class BeregningYrkeskaderBeløpVurdering(
    @JsonIgnore val id: Long? = null,
    val vurderinger: List<YrkesskadeBeløpVurdering>
)

data class YrkesskadeBeløpVurdering(
    val antattÅrligInntekt: Beløp,
    val referanse: String,
    val begrunnelse: String
)