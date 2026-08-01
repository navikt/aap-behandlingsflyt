package no.nav.aap.beregning

import com.fasterxml.jackson.annotation.JsonIgnore

data class BeregningYrkeskaderBeløpVurdering(
    @JsonIgnore val id: Long? = null,
    val vurderinger: List<YrkesskadeBeløpVurdering>
)