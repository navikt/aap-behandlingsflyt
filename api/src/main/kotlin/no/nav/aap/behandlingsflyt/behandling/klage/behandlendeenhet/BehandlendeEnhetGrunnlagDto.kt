package no.nav.aap.behandlingsflyt.behandling.klage.behandlendeenhet

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetVurdering

data class BehandlendeEnhetGrunnlagDto(
    val vurdering: BehandlendeEnhetVurderingDto?,
    val harTilgangTilÅSaksbehandle: Boolean,
)

data class BehandlendeEnhetVurderingDto(
    val skalBehandlesAvNay: Boolean,
    val skalBehandlesAvKontor: Boolean,
    val vurdertAv: VurdertAvResponse?
)

internal fun BehandlendeEnhetVurdering.tilDto(ansattInfoService: AnsattInfoService) =
    BehandlendeEnhetVurderingDto(
        skalBehandlesAvNay = skalBehandlesAvNay,
        skalBehandlesAvKontor = skalBehandlesAvKontor,
        vurdertAv = VurdertAvResponse.fraIdent(vurdertAv, opprettet, ansattInfoService),
    )