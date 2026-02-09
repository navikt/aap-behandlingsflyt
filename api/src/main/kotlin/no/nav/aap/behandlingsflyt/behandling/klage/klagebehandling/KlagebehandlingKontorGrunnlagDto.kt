package no.nav.aap.behandlingsflyt.behandling.klage.klagebehandling

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagebehandlingKontorGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontor
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

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
    val vurdertAv: VurdertAvResponse?,
    val kvalitetssikretAv: VurdertAvResponse?,
)

internal fun KlagevurderingKontor.tilDto(vurdertAvService: VurdertAvService, behandlingId: BehandlingId) =
    KlagevurderingKontorDto(
        begrunnelse = begrunnelse,
        notat = notat,
        innstilling = innstilling,
        vilkårSomOpprettholdes = vilkårSomOpprettholdes,
        vilkårSomOmgjøres = vilkårSomOmgjøres,
        vurdertAv = vurdertAvService.medNavnOgEnhet(vurdertAv, requireNotNull(opprettet) {
            "Opprettet-tidspunkt kan ikke være null"
        }),
        kvalitetssikretAv = vurdertAvService.kvalitetssikretAv(Definisjon.VURDER_KLAGE_KONTOR, behandlingId)
    )

internal fun KlagebehandlingKontorGrunnlag.tilDto(
    harTilgangTilÅSaksbehandle: Boolean,
    vurdertAvService: VurdertAvService,
    behandlingId: BehandlingId
) =
    KlagebehandlingKontorGrunnlagDto(
        vurdering = vurdering.tilDto(vurdertAvService, behandlingId),
        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle
    )