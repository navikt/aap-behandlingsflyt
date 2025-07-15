package no.nav.aap.behandlingsflyt.behandling.klage.fullmektig

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.IdentMedType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.NavnOgAdresse
import java.time.Instant


data class FullmektigGrunnlagDto(
    val vurdering: FullmektigVurderingDto? = null,
    val harTilgangTilÅSaksbehandle: Boolean
)

data class FullmektigVurderingDto(
    val harFullmektig: Boolean,
    val fullmektigIdentMedType: IdentMedType? = null,
    val fullmektigIdent: String? = null,
    val fullmektigNavnOgAdresse: NavnOgAdresse? = null,
    val vurdertAv: VurdertAvResponse?
)

internal fun FullmektigGrunnlag.tilDto(harTilgangTilÅSaksbehandle: Boolean): FullmektigGrunnlagDto {
    return FullmektigGrunnlagDto(
        vurdering = this.vurdering.tilDto(),
        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle
    )
}

internal fun FullmektigVurdering.tilDto(): FullmektigVurderingDto {
    return FullmektigVurderingDto(
        harFullmektig = this.harFullmektig,
        fullmektigIdent = fullmektigIdent?.ident,
        fullmektigIdentMedType = this.fullmektigIdent,
        fullmektigNavnOgAdresse = this.fullmektigNavnOgAdresse,
        vurdertAv = VurdertAvResponse.fraIdent(this.vurdertAv, this.opprettet)
    )
}

