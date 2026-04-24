package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode

data class VedtakslengdeVedtakDto(
    val periode: Periode,
    val rettighetsType: RettighetsType?,
    val utfall: Utfall,
)

data class VedtakslengdeVedtakResponse(
    val perioder: List<VedtakslengdeVedtakDto>?,
    val stansOpphør: List<StansOpphørDto>,
)
