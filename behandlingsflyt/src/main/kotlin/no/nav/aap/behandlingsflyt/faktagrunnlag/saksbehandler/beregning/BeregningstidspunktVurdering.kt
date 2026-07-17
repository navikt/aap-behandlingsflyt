package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate
import java.time.LocalDateTime

enum class ÅrsakBeregningstidspunkt {
    SYKEMELDINGSDATO,
    KRAVDATO,
    DATO_PAA_LEGEERKLÆRING,
    HENVIST_TIL_BEHANDLING,
    SEKSTEN_ÅR_SOM_BEREGNINGSTIDSPUNKT,
    ANNET,
}

enum class ÅrsakYtterligereNedsatt {
    UFØRETIDSPUNKT,
    YTTERLIGERE_NEDSATT,
    ØKT_UFØREGRAD,
    IKKE_BETYDNING_IKKE_RELEVANT,
    ANNET,
}

/**
 * Se [regelspesifiseringen](https://confluence.adeo.no/spaces/PAAP/pages/514473196/%C2%A7+11-28.+Forholdet+til+andre+reduserte+ytelser+fra+folketrygden) for begrepsbruk.
 */
data class BeregningstidspunktVurdering(
    @JsonIgnore val id: Long? = null,
    val begrunnelse: String,
    val nedsattArbeidsevneEllerStudieevneDato: LocalDate,
    val ytterligereNedsattBegrunnelse: String?,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
    val vurdertAv: Bruker,
    val vurdertTidspunkt: LocalDateTime? = null,
    val årsak: ÅrsakBeregningstidspunkt? = null,
    val ytterligereNedsattÅrsak: ÅrsakYtterligereNedsatt? = null,
)

data class BeregningstidspunktVurderingDto(
    val begrunnelse: String,
    val nedsattArbeidsevneDato: LocalDate,
    val ytterligereNedsattBegrunnelse: String?,
    val ytterligereNedsattArbeidsevneDato: LocalDate?,
    val årsak: ÅrsakBeregningstidspunkt? = null,
    val ytterligereNedsattÅrsak: ÅrsakYtterligereNedsatt? = null,
)