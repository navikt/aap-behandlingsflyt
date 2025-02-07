package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.fritakmeldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritaksvurderingDto
import java.time.LocalDate
import java.time.LocalDateTime

data class FritakMeldepliktGrunnlagDto(
    val historikk: Set<FritakMeldepliktVurderingDto>,
    val gjeldendeVedtatteVurderinger: List<FritakMeldepliktVurderingDto>,
    val vurderinger: List<FritakMeldepliktVurderingDto>
)

data class SimulerFritakMeldepliktDto(val fritaksvurderinger: List<FritaksvurderingDto>)

data class SimulertFritakMeldepliktDto(
    val gjeldendeVedtatteVurderinger: List<FritakMeldepliktVurderingDto>
)

data class FritakMeldepliktVurderingDto(
    val begrunnelse: String,
    val vurderingsTidspunkt: LocalDateTime,
    val harFritak: Boolean,
    val fraDato: LocalDate
)

