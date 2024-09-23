package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritakMeldepliktGrunnlagDto
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

data class MeldepliktGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurderinger: List<Fritaksvurdering>
) {
    fun toDto(): FritakMeldepliktGrunnlagDto {
        return FritakMeldepliktGrunnlagDto(vurderinger.map(Fritaksvurdering::toDto))
    }
}
