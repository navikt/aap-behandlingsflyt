package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Se [regelspesifiseringen](https://confluence.adeo.no/spaces/PAAP/pages/514473196/%C2%A7+11-28.+Forholdet+til+andre+reduserte+ytelser+fra+folketrygden) for begrepsbruk.
 */
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