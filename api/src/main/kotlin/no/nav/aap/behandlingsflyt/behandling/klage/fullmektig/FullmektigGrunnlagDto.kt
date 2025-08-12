package no.nav.aap.behandlingsflyt.behandling.klage.fullmektig

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.IdentMedType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.NavnOgAdresse

data class FullmektigGrunnlagDto(
    val vurdering: FullmektigVurderingDto? = null,
    val harTilgangTilÅSaksbehandle: Boolean
)

data class FullmektigVurderingDto(
    val harFullmektig: Boolean,
    val fullmektigIdentMedType: IdentMedType? = null,
    @Deprecated("Bruk fullmektigIdentMedType")
    val fullmektigIdent: String? = null,
    val fullmektigNavnOgAdresse: NavnOgAdresse? = null,
    val vurdertAv: VurdertAvResponse?
)

internal fun FullmektigGrunnlag.tilDto(
    harTilgangTilÅSaksbehandle: Boolean,
    ansattInfoService: AnsattInfoService,
): FullmektigGrunnlagDto {
    return FullmektigGrunnlagDto(
        vurdering = this.vurdering.tilDto(ansattInfoService),
        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle
    )
}

internal fun FullmektigVurdering.tilDto(ansattInfoService: AnsattInfoService): FullmektigVurderingDto {
    return FullmektigVurderingDto(
        harFullmektig = this.harFullmektig,
        fullmektigIdent = fullmektigIdent?.ident,
        fullmektigIdentMedType = this.fullmektigIdent,
        fullmektigNavnOgAdresse = this.fullmektigNavnOgAdresse,
        vurdertAv = VurdertAvResponse.fraIdent(this.vurdertAv, this.opprettet, ansattInfoService)
    )
}

