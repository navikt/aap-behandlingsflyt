package no.nav.aap.behandlingsflyt.behandling.klage.klagebehandling

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagebehandlingKontorGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontor

data class KlagebehandlingKontorGrunnlagDto(
    val vurdering: KlagevurderingKontorDto? = null,
    val harTilgangTilÅSaksbehandle: Boolean
)

data class KlagevurderingKontorDto(
    val begrunnelse: String,
    val notat: String?,
    val innstilling: KlageInnstilling,
    val vilkårSomOpprettholdes: List<Hjemmel>,
    val vilkårSomOmgjøres: List<Hjemmel>,
    val vurdertAv: VurdertAvResponse?
)

internal fun KlagevurderingKontor.tilDto(ansattInfoService: AnsattInfoService) =
    KlagevurderingKontorDto(
        begrunnelse = begrunnelse,
        notat = notat,
        innstilling = innstilling,
        vilkårSomOpprettholdes = vilkårSomOpprettholdes,
        vilkårSomOmgjøres = vilkårSomOmgjøres,
        vurdertAv = VurdertAvResponse.fraIdent(vurdertAv, opprettet, ansattInfoService),
    )

internal fun KlagebehandlingKontorGrunnlag.tilDto(
    harTilgangTilÅSaksbehandle: Boolean,
    ansattInfoService: AnsattInfoService,
) =
    KlagebehandlingKontorGrunnlagDto(
        vurdering = vurdering.tilDto(ansattInfoService),
        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle
    )