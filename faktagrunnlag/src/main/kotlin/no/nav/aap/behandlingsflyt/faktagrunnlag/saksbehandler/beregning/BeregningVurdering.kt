package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.verdityper.Beløp
import java.time.LocalDate

data class BeregningVurdering(
    val begrunnelse: String,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
    val antattÅrligInntekt: Beløp?
)