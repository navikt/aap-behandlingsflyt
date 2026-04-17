package no.nav.aap.behandlingsflyt.behandling.rettighetsperiode

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeHarRett
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate

data class RettighetsperiodeGrunnlagResponse(
    val vurdering: RettighetsperiodeVurderingDto?,
    val søknadsdato: LocalDate?,
    val harTilgangTilÅSaksbehandle: Boolean
)

data class RettighetsperiodeVurderingDto(
    val begrunnelse: String,
    val harRett: RettighetsperiodeHarRett,
    val startDato: LocalDate?,
    val vurdertAv: VurdertAvResponse,
    val besluttetAv: VurdertAvResponse? = null,
)

fun RettighetsperiodeVurdering.tilDto(
    vurdertAvService: VurdertAvService,
    behandlingId: BehandlingId
): RettighetsperiodeVurderingDto {
    return RettighetsperiodeVurderingDto(
        begrunnelse = begrunnelse,
        harRett = harRettUtoverSøknadsdato,
        startDato = startDato,
        vurdertAv = vurdertAvService.medNavnOgEnhet(vurdertAv, vurdertDato),
        besluttetAv = vurdertAvService.besluttetAv(
            definisjon = Definisjon.VURDER_RETTIGHETSPERIODE,
            behandlingId = behandlingId,
        ),
    )
}
