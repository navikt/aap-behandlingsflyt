package no.nav.aap.behandlingsflyt.behandling.barnepensjon

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.verdityper.Beløp

data class BarnepensjonGrunnlagDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: BarnepensjonVurderingDto?,
    val historiskeVurderinger: List<BarnepensjonVurderingDto>
) {
    companion object {
        fun fraDomene(
            kanSaksbehandle: Boolean,
            vurdertAvService: VurdertAvService,
            grunnlag: BarnepensjonGrunnlag?,
            historiskeVurderinger: List<BarnepensjonGrunnlag>
        ): BarnepensjonGrunnlagDto {
            return BarnepensjonGrunnlagDto(
                harTilgangTilÅSaksbehandle = kanSaksbehandle,
                vurdering = grunnlag?.vurdering?.let {
                    BarnepensjonVurderingDto.fraDomene(it, vurdertAvService)
                },
                historiskeVurderinger = historiskeVurderinger.map {
                    BarnepensjonVurderingDto.fraDomene(
                        it.vurdering,
                        vurdertAvService
                    )
                }
            )
        }
    }
}

data class BarnepensjonVurderingDto(
    val perioder: List<BarnepensjonVurderingPeriodeDto>,
    val begrunnelse: String,
    val vurderingerMeta: VurderingerMetaResponse,
) {
    companion object {
        fun fraDomene(
            vurdering: BarnepensjonVurdering,
            vurdertAvService: VurdertAvService
        ): BarnepensjonVurderingDto {
            val vurdertAv = vurdering.vurdertAv.ident
            return BarnepensjonVurderingDto(
                perioder = vurdering.perioder.map { periode ->
                    BarnepensjonVurderingPeriodeDto(
                        fom = periode.fom.toString(),
                        tom = periode.tom?.toString(),
                        månedsbeløp = periode.månedsats
                    )
                },
                begrunnelse = vurdering.begrunnelse,
                vurderingerMeta = vurdertAvService.vurderingerMeta(
                    definisjon = Definisjon.SAMORDNING_BARNEPENSJON,
                    behandlingId = vurdering.vurdertIBehandling,
                    vurdertAv = vurdertAvService.medNavnOgEnhet(vurdertAv, vurdering.opprettet),
                ),
            )
        }
    }
}

data class BarnepensjonVurderingPeriodeDto(
    val fom: String,
    val tom: String?,
    val månedsbeløp: Beløp,
)