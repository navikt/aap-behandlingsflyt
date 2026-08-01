package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.aap.komponenter.verdityper.Beløp

data class BeregningYrkeskaderBeløpVurderingDTO(
    @JsonIgnore val id: Long? = null,
    val vurderinger: List<YrkesskadeBeløpVurderingDTO>
)

data class YrkesskadeBeløpVurderingDTO(
    val antattÅrligInntekt: Beløp,
    val referanse: String,
    val begrunnelse: String,
)