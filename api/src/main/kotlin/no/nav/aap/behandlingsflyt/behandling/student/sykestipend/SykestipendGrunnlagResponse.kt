package no.nav.aap.behandlingsflyt.behandling.student.sykestipend

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.type.Periode

data class SykestipendGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val historiskeVurderinger: List<SykestipendvurderingResponse>,
    val gjeldendeVurdering: SykestipendvurderingResponse?,
    val sykeStipendSvarFraSøknad: Boolean? = null
)

data class SykestipendvurderingResponse(
    val begrunnelse: String,
    val perioder: List<Periode>,
    val vurderingerMeta: VurderingerMetaResponse,
) {

    companion object {
        fun fraDomene(sykestipendVurdering: SykestipendVurdering, vurdertAvService: VurdertAvService) =
            SykestipendvurderingResponse(
                begrunnelse = sykestipendVurdering.begrunnelse,
                perioder = sykestipendVurdering.perioder.sortedBy { it.fom },
                vurderingerMeta = vurdertAvService.vurderingerMeta(
                    definisjon = Definisjon.AVKLAR_SAMORDNING_SYKESTIPEND,
                    behandlingId = sykestipendVurdering.vurdertIBehandling,
                    vurdertAv = vurdertAvService.medNavnOgEnhet(
                        sykestipendVurdering.vurdertAv.ident,
                        sykestipendVurdering.opprettet,
                    ),
                )
            )
    }
}
