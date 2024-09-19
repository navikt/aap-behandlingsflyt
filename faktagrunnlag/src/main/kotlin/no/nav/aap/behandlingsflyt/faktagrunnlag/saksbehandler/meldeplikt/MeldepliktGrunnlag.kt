package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.flate.PeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritakMeldepliktGrunnlagDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritakMeldepliktVurderingDto
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDateTime

data class MeldepliktGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId
    val vurderinger: List<Fritaksvurdering>
) {
    fun toDto() = FritakMeldepliktGrunnlagDto(
        vurdering.begrunnelse, vurdering.fritaksPerioder.map(::toFritakMeldepliktVurderingDto), opprettetTid
    )

    private fun toFritakMeldepliktVurderingDto(fritaksPeriode: Fritaksperiode) = FritakMeldepliktVurderingDto(
        fritaksPeriode.harFritak, PeriodeDto(fritaksPeriode.periode)
    )
}
