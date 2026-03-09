package no.nav.aap.behandlingsflyt.behandling.barnepensjon

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonVurdering
import no.nav.aap.komponenter.verdityper.Beløp
import java.time.YearMonth

data class BarnepensjonGrunnlagDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: BarnepensjonVurderingDto?,
) {
    companion object {
        fun fraDomene(
            kanSaksbehandle: Boolean,
            vurdertAvService: VurdertAvService,
            grunnlag: BarnepensjonGrunnlag?
        ): BarnepensjonGrunnlagDto {
            return BarnepensjonGrunnlagDto(
                harTilgangTilÅSaksbehandle = kanSaksbehandle,
                vurdering = grunnlag?.vurdering?.let {
                    BarnepensjonVurderingDto.fraDomene(it, vurdertAvService)
                }
            )
        }
    }
}

data class BarnepensjonVurderingDto(
    val perioder: List<BarnepensjonVurderingPeriodeDto>,
    val begrunnelse: String,
    val vurdertAv: VurdertAvResponse
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
                        fom = periode.fom,
                        tom = periode.tom,
                        månedsbeløp = periode.månedsats
                    )
                },
                begrunnelse = vurdering.begrunnelse,
                vurdertAv = vurdertAvService.medNavnOgEnhet(vurdertAv, vurdering.opprettet)
            )
        }
    }
}

data class BarnepensjonVurderingPeriodeDto(
    val fom: YearMonth,
    val tom: YearMonth?,
    val månedsbeløp: Beløp,
)