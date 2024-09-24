package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.flate.PeriodeDto
import java.time.LocalDateTime

data class FritakMeldepliktGrunnlagDto(
    val vurderinger: List<FritakMeldepliktVurderingDto>
)

data class FritakMeldepliktVurderingDto(
    val begrunnelse: String,
    val vurderingsTidspunkt: LocalDateTime,
    val fritaksperioder: List<MeldepliktFritaksperiodeDto>
)

data class MeldepliktFritaksperiodeDto(
    val periode: PeriodeDto,
    val harFritak: Boolean
)

