package no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerKilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerYtelseType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerKilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerYtelseType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UførePeriodeMedEndringStatus

fun mapSamordningAndreStatligeYtelserVurderingDTO(
    vurdering: SamordningAndreStatligeYtelserVurdering,
    behandlingId: BehandlingId,
    vurdertAvService: VurdertAvService,
): SamordningAndreStatligeYtelserVurderingDTO =
    SamordningAndreStatligeYtelserVurderingDTO(
        begrunnelse = vurdering.begrunnelse,
        vurderingPerioder = vurdering.vurderingPerioder
            .map {
                SamordningAndreStatligeYtelserVurderingPeriodeDTO(
                    periode = it.periode,
                    ytelse = it.ytelse,
                )
            },
        vurderingerMeta = vurdertAvService.byggVurderingerMeta(
            definisjon = Definisjon.SAMORDNING_ANDRE_STATLIGE_YTELSER,
            behandlingId = behandlingId,
            vurdertAv = vurdertAvService.medNavnOgEnhet(
                ident = vurdering.vurdertAv,
                dato = requireNotNull(vurdering.vurdertTidspunkt?.toLocalDate()) {
                    "Fant ikke vurdert tidspunkt for samordningAndreStatligeYtelserVurdering"
                },
            ),
        ),
    )

fun mapSamordningArbeidsgiverVurdering(
    vurdering: SamordningArbeidsgiverVurdering,
    behandlingId: BehandlingId,
    vurdertAvService: VurdertAvService,
): SamordningArbeidsgiverVurderingDTO =
    SamordningArbeidsgiverVurderingDTO(
        begrunnelse = vurdering.begrunnelse,
        perioder = vurdering.perioder,
        vurderingerMeta = vurdertAvService.byggVurderingerMeta(
            definisjon = Definisjon.SAMORDNING_ARBEIDSGIVER,
            behandlingId = behandlingId,
            vurdertAv = vurdertAvService.medNavnOgEnhet(
                ident = vurdering.vurdertAv,
                dato = requireNotNull(vurdering.vurdertTidspunkt?.toLocalDate()) {
                    "Fant ikke vurdert tidspunkt for samordningArbeidsgiverVurdering"
                },
            ),
        ),
    )

fun mapSamordningVurdering(
    samordning: SamordningVurderingGrunnlag,
    behandlingId: BehandlingId,
    vurdertAvService: VurdertAvService,
): SamordningYtelseVurderingDTO {
    return SamordningYtelseVurderingDTO(
        begrunnelse = samordning.begrunnelse,
        vurderinger = samordning.vurderinger.flatMap { vurdering ->
            vurdering.vurderingPerioder.map {
                SamordningVurderingDTO(
                    ytelseType = vurdering.ytelseType,
                    gradering = it.gradering?.prosentverdi(),
                    periode = it.periode,
                    kronesum = it.kronesum?.toInt(),
                    manuell = it.manuell
                )
            }
        },
        vurderingerMeta = vurdertAvService.byggVurderingerMeta(
            definisjon = Definisjon.AVKLAR_SAMORDNING_GRADERING,
            behandlingId = behandlingId,
            vurdertAv = vurdertAvService.medNavnOgEnhet(
                ident = samordning.vurdertAv,
                dato = requireNotNull(samordning.vurdertTidspunkt?.toLocalDate()) {
                    "Fant ikke vurderingstidspunkt for yrkesskadevurdering"
                },
            ),
        ),
    )
}

fun mapSamordningUføreVurdering(
    vurdering: SamordningUføreVurdering?,
    behandlingId: BehandlingId,
    vurdertAvService: VurdertAvService,
): SamordningUføreVurderingDTO? =
    vurdering?.let {
        return SamordningUføreVurderingDTO(
            begrunnelse = it.begrunnelse,
            vurderingPerioder =
                it.vurderingPerioder.map { periode ->
                    SamordningUføreVurderingPeriodeDTO(
                        periode.virkningstidspunkt,
                        periode.uføregradTilSamordning.prosentverdi()
                    )
                },
            vurderingerMeta = vurdertAvService.byggVurderingerMeta(
                definisjon = Definisjon.AVKLAR_SAMORDNING_UFØRE,
                behandlingId = behandlingId,
                vurdertAv = vurdertAvService.medNavnOgEnhet(
                    ident = it.vurdertAv,
                    dato = requireNotNull(it.vurdertTidspunkt?.toLocalDate()) {
                        "Fant ikke vurderingstidspunkt for samordning uføre"
                    },
                ),
            ),
        )
    }

fun mapSamordningUføreGrunnlag(
    registerGrunnlagVurderinger: List<UførePeriodeMedEndringStatus>
): List<SamordningUføreGrunnlagDTO> =
    registerGrunnlagVurderinger.map {
        SamordningUføreGrunnlagDTO(
            virkningstidspunkt = it.virkningstidspunkt,
            uføregrad = it.uføregrad.prosentverdi(),
            endringStatus = it.endringStatus
        )
    }

fun mapDagpengerKilde(kilde: DagpengerKilde): AndreStatligeYtelserKilde {
    return when (kilde) {
        DagpengerKilde.ARENA -> AndreStatligeYtelserKilde.ARENA
        DagpengerKilde.DP_SAK -> AndreStatligeYtelserKilde.DP_SAK
    }
}

fun mapDagpengerYtelseType(ytelseType: DagpengerYtelseType): AndreStatligeYtelserType {
    return when (ytelseType) {
        DagpengerYtelseType.DAGPENGER_ARBEIDSSOKER_ORDINAER -> AndreStatligeYtelserType.DAGPENGER_ARBEIDSSOKER_ORDINAER
        DagpengerYtelseType.DAGPENGER_PERMITTERING_ORDINAER -> AndreStatligeYtelserType.DAGPENGER_PERMITTERING_ORDINAER
        DagpengerYtelseType.DAGPENGER_PERMITTERING_FISKEINDUSTRI -> AndreStatligeYtelserType.DAGPENGER_PERMITTERING_FISKEINDUSTRI
    }
}

fun mapTiltakspengerKilde(kilde: TiltakspengerKilde): AndreStatligeYtelserKilde {
    return when (kilde) {
        TiltakspengerKilde.TPSAK -> AndreStatligeYtelserKilde.TPSAK
        TiltakspengerKilde.ARENA -> AndreStatligeYtelserKilde.ARENA
    }
}

fun mapTiltakspengerYtelseType(ytelseType: TiltakspengerYtelseType): AndreStatligeYtelserType {
    return when (ytelseType) {
        TiltakspengerYtelseType.TILTAKSPENGER -> AndreStatligeYtelserType.TILTAKSPENGER
        TiltakspengerYtelseType.TILTAKSPENGER_OG_BARNETILLEGG -> AndreStatligeYtelserType.TILTAKSPENGER_OG_BARNETILLEGG
        TiltakspengerYtelseType.INGENTING -> AndreStatligeYtelserType.TILTAKSPENGER_INAKTIV
    }
}