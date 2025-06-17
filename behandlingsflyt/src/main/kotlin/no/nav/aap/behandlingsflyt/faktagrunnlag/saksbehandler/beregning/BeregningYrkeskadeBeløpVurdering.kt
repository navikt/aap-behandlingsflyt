package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.aap.komponenter.verdityper.Beløp
import java.time.LocalDateTime

data class BeregningYrkeskaderBeløpVurdering(
    @JsonIgnore val id: Long? = null,
    val vurderinger: List<YrkesskadeBeløpVurdering>
)

data class YrkesskadeBeløpVurdering(
    val antattÅrligInntekt: Beløp,
    val referanse: String,
    val begrunnelse: String,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null
)

data class BeregningYrkeskaderBeløpVurderingDTO(
    @JsonIgnore val id: Long? = null,
    val vurderinger: List<YrkesskadeBeløpVurderingDTO>
)

data class YrkesskadeBeløpVurderingDTO(
    val antattÅrligInntekt: Beløp,
    val referanse: String,
    val begrunnelse: String,
)