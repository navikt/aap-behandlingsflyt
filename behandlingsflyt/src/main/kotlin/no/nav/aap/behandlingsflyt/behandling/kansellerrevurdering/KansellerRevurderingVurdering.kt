package no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering

import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.flate.KansellerRevurderingVurderingDto
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.flate.KansellerRevurderingÅrsakDto
import no.nav.aap.komponenter.verdityper.Bruker

data class KansellerRevurderingVurdering(
    val årsak: KansellerRevurderingÅrsak?,
    val begrunnelse: String,
    val vurdertAv: Bruker
)

enum class KansellerRevurderingÅrsak {
    FEILREGISTRERING,
    START_REVURDERING_PAA_NYTT
}

fun KansellerRevurderingVurdering.tilDto(): KansellerRevurderingVurderingDto =
    KansellerRevurderingVurderingDto(
        årsak = this.årsak?.let { KansellerRevurderingÅrsakDto.valueOf(it.name) },
        begrunnelse = this.begrunnelse
    )
