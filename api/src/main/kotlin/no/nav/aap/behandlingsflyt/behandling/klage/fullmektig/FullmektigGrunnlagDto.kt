package no.nav.aap.behandlingsflyt.behandling.klage.fullmektig

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.NavnOgAdresse
import java.time.Instant


data class FullmektigGrunnlagDto(
    val vurdering: FullmektigVurderingDto? = null
)

data class FullmektigVurderingDto(
    val harFullmektig: Boolean,
    val fullmektigIdent: String? = null,
    val fullmektigNavnOgAdresse: NavnOgAdresse? = null,
    val vurdertAv: String,
    val opprettet: Instant
)

internal fun FullmektigGrunnlag.tilDto(): FullmektigGrunnlagDto {
    return FullmektigGrunnlagDto(
        vurdering = this.vurdering.tilDto()
    )
}

internal fun FullmektigVurdering.tilDto(): FullmektigVurderingDto {
    return FullmektigVurderingDto(
        harFullmektig = this.harFullmektig,
        fullmektigIdent = this.fullmektigIdent,
        fullmektigNavnOgAdresse = this.fullmektigNavnOgAdresse,
        vurdertAv = this.vurdertAv,
        opprettet = this.opprettet!!
    )
}

