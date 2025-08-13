package no.nav.aap.behandlingsflyt.behandling.klage.klagebehandling

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNay

data class KlagebehandlingNayGrunnlagDto(
    val vurdering: KlagevurderingNayDto? = null,
    val harTilgangTilÅSaksbehandle: Boolean
)

data class KlagevurderingNayDto(
    val begrunnelse: String,
    val notat: String?,
    val innstilling: KlageInnstilling,
    val vilkårSomOpprettholdes: List<Hjemmel>,
    val vilkårSomOmgjøres: List<Hjemmel>,
    val vurdertAv: VurdertAvResponse?
)

internal fun KlagevurderingNay.tilDto(ansattInfoService: AnsattInfoService) =
    KlagevurderingNayDto(
        begrunnelse = begrunnelse,
        notat = notat,
        innstilling = innstilling,
        vilkårSomOpprettholdes = vilkårSomOpprettholdes,
        vilkårSomOmgjøres = vilkårSomOmgjøres,
        vurdertAv = VurdertAvResponse.fraIdent(vurdertAv, opprettet, ansattInfoService),
    )

internal fun KlagebehandlingNayGrunnlag.tilDto(
    harTilgangTilÅSaksbehandle: Boolean,
    ansattInfoService: AnsattInfoService,
) =
    KlagebehandlingNayGrunnlagDto(
        vurdering = vurdering.tilDto(ansattInfoService),
        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle
    )