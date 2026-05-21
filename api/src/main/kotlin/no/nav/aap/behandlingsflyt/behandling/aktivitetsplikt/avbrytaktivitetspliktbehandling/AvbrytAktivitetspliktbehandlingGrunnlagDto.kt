package no.nav.aap.behandlingsflyt.behandling.aktivitetsplikt.avbrytaktivitetspliktbehandling

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingÅrsakDto

data class AvbrytAktivitetspliktbehandlingGrunnlagDto(
    val vurdering: AvbrytAktivitetspliktbehandlingVurderingDto?,
)

data class AvbrytAktivitetspliktbehandlingVurderingDto(
    val årsak: AvbrytAktivitetspliktbehandlingÅrsakDto,
    val begrunnelse: String,
    val vurderingerMeta: VurderingerMetaResponse
)