package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate

data class BeregningstidspunktVurdering(
    @JsonIgnore internal val id: Long? = null,
    val begrunnelse: String,
    val nedsattArbeidsevneDato: LocalDate,
    val ytterligereNedsattBegrunnelse: String?,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
)