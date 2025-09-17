package no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.flate.AvbrytRevurderingVurderingDto
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.flate.AvbrytRevurderingÅrsakDto
import no.nav.aap.komponenter.verdityper.Bruker

data class AvbrytRevurderingVurdering(
    val årsak: AvbrytRevurderingÅrsak?,
    val begrunnelse: String,
    val vurdertAv: Bruker
)

enum class AvbrytRevurderingÅrsak {
    REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL,
    DET_HAR_OPPSTAATT_EN_FEIL_OG_BEHANDLINGEN_MAA_STARTES_PAA_NYTT
}

fun AvbrytRevurderingVurdering.tilDto(): AvbrytRevurderingVurderingDto =
    AvbrytRevurderingVurderingDto(
        årsak = this.årsak?.let { AvbrytRevurderingÅrsakDto.valueOf(it.name) },
        begrunnelse = this.begrunnelse
    )
