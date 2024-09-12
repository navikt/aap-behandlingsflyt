package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.flate.PeriodeDto

data class FritakMeldepliktGrunnlagDto(
    val begrunnelse: String,
    val vurderinger: List<FritakMeldepliktVurderingDto>
)

data class FritakMeldepliktVurderingDto(
    val harFritak: Boolean,
    val periode: PeriodeDto
)
