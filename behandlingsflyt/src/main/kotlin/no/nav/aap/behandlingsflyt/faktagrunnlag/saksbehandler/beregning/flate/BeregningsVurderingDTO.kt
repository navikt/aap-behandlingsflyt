package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.flate

import java.math.BigDecimal
import java.time.LocalDate

data class BeregningsVurderingDTO(
    val begrunnelse: String?,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
    val antattÅrligInntekt: BigDecimal?,
)