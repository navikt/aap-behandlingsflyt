package no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattNavnOgEnhet
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UførePeriodeMedEndringStatus
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle

fun mapSamordningAndreStatligeYtelserVurderingDTO(
    vurdering: SamordningAndreStatligeYtelserVurdering,
    navnOgEnhet: AnsattNavnOgEnhet?
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
        vurdertAv =
            vurdering.let {
                VurdertAvResponse(
                    ident = it.vurdertAv,
                    dato = requireNotNull(it.vurdertTidspunkt?.toLocalDate()) {
                        "Fant ikke vurdert tidspunkt for samordningAndreStatligeYtelserVurdering"
                    },
                    ansattnavn = navnOgEnhet?.navn,
                    enhetsnavn = navnOgEnhet?.enhet
                )
            }
    )

fun mapSamordningArbeidsgiverVurdering(
    vurdering: SamordningArbeidsgiverVurdering,
    navnOgEnhet: AnsattNavnOgEnhet?
): SamordningArbeidsgiverVurderingDTO =
    SamordningArbeidsgiverVurderingDTO(
        begrunnelse = vurdering.begrunnelse,
        perioder = vurdering.perioder,
        vurdertAv = VurdertAvResponse(
            ident = vurdering.vurdertAv,
            dato = requireNotNull(vurdering.vurdertTidspunkt?.toLocalDate()) {
                "Fant ikke vurdert tidspunkt for samordningArbeidsgiverVurdering"
            },
            ansattnavn = navnOgEnhet?.navn,
            enhetsnavn = navnOgEnhet?.enhet
        )
    )

fun mapSamordningVurdering(
    samordning: SamordningVurderingGrunnlag,
    ansattInfoService: AnsattInfoService
): SamordningYtelseVurderingDTO {
    val ansattNavnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(samordning.vurdertAv)

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
        vurdertAv = VurdertAvResponse(
            ident = samordning.vurdertAv,
            dato = requireNotNull(samordning.vurdertTidspunkt?.toLocalDate()) {
                "Fant ikke vurderingstidspunkt for yrkesskadevurdering"
            },
            ansattnavn = ansattNavnOgEnhet?.navn,
            enhetsnavn = ansattNavnOgEnhet?.enhet
        ),
    )
}

fun mapSamordningUføreVurdering(
    vurdering: SamordningUføreVurdering?,
    ansattInfoService: AnsattInfoService,
): SamordningUføreVurderingDTO? =
    vurdering?.let {
        val navnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(it.vurdertAv)

        return SamordningUføreVurderingDTO(
            begrunnelse = it.begrunnelse,
            vurderingPerioder =
                it.vurderingPerioder.map { periode ->
                    SamordningUføreVurderingPeriodeDTO(
                        periode.virkningstidspunkt,
                        periode.uføregradTilSamordning.prosentverdi()
                    )
                },
            vurdertAv = VurdertAvResponse(
                ident = it.vurdertAv,
                dato = requireNotNull(it.vurdertTidspunkt?.toLocalDate()) {
                    "Fant ikke vurderingstidspunkt for samordning uføre"
                },
                ansattnavn = navnOgEnhet?.navn,
                enhetsnavn = navnOgEnhet?.enhet
            )
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