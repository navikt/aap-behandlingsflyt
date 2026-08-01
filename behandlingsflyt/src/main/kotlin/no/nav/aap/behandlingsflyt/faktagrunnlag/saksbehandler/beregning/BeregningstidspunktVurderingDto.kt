package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import java.time.LocalDate
import no.nav.aap.beregning.ÅrsakBeregningstidspunkt
import no.nav.aap.beregning.ÅrsakYtterligereNedsatt

data class BeregningstidspunktVurderingDto(
    val begrunnelse: String,
    val nedsattArbeidsevneDato: LocalDate,
    val ytterligereNedsattBegrunnelse: String?,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
    val årsak: ÅrsakBeregningstidspunkt? = null,
    val ytterligereNedsattÅrsak: ÅrsakYtterligereNedsatt? = null,
)