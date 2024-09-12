package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.flate.PeriodeDto
import java.time.LocalDateTime

data class FritakMeldepliktGrunnlagDto(
    val begrunnelse: String,
    val vurderinger: List<FritakMeldepliktVurderingDto>,
    val vurderingsTidspunkt: LocalDateTime
)

data class FritakMeldepliktVurderingDto(
    val harFritak: Boolean,
    val periode: PeriodeDto
)
