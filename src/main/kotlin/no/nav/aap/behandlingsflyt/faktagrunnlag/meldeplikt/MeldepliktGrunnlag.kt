package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt

import no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt.Fritaksvurdering

class MeldepliktGrunnlag(
    val id: Long,
    val behandlingId: Long,
    val vurderinger: List<Fritaksvurdering>
)