package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate
import java.time.LocalDateTime

data class BeregningstidspunktVurdering(
    @JsonIgnore val id: Long? = null,
    val begrunnelse: String,
    val nedsattArbeidsevneDato: LocalDate,
    val ytterligereNedsattBegrunnelse: String?,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null,
)

data class BeregningstidspunktVurderingDto(
    val begrunnelse: String,
    val nedsattArbeidsevneDato: LocalDate,
    val ytterligereNedsattBegrunnelse: String?,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
)