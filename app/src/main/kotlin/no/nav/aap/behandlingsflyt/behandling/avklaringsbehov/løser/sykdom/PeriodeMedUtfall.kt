package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.sykdom

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode

data class PeriodeMedUtfall(
    @JsonProperty("periode") val periode: Periode,
    @JsonProperty("utfall") val utfall: Utfall,
    @JsonProperty("begrunnelse") val begrunnelse: String
)