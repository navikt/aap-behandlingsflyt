package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.komponenter.verdityper.Beløp
import java.time.LocalDate

data class BeregningVurdering(
    val begrunnelse: String,
    val nedsattArbeidsevneDato: LocalDate,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
    val antattÅrligInntekt: Beløp?
)