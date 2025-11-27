package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLand
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate


data class PeriodisertLovvalgMedlemskapGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    override val sisteVedtatteVurderinger: List<PeriodisertManuellVurderingForLovvalgMedlemskapResponse>,
    override val nyeVurderinger: List<PeriodisertManuellVurderingForLovvalgMedlemskapResponse>,
    override val kanVurderes: List<Periode>,
    override val behøverVurderinger: List<Periode>,
    val overstyrt: Boolean = false
): PeriodiserteVurderingerDto<PeriodisertManuellVurderingForLovvalgMedlemskapResponse>

data class PeriodisertManuellVurderingForLovvalgMedlemskapResponse(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertAv: VurdertAvResponse?,
    override val kvalitetssikretAv: VurdertAvResponse? = null,
    override val besluttetAv: VurdertAvResponse? = null,
    val lovvalg: LovvalgResponse,
    val medlemskap: MedlemskapResponse?,
    val overstyrt: Boolean = false,
): VurderingDto

data class LovvalgResponse(
    val begrunnelse: String,
    val lovvalgsLandEllerLandMedAvtale: EØSLandEllerLandMedAvtale
)

data class MedlemskapResponse(
    val begrunnelse: String,
    val varMedlemIFolketrygd: Boolean
)

fun ManuellVurderingForLovvalgMedlemskap.toResponse(
    vurdertAvService: VurdertAvService,
    fom: LocalDate = this.fom,
    tom: LocalDate? = this.tom,
) =
    PeriodisertManuellVurderingForLovvalgMedlemskapResponse(
        fom = fom,
        tom = tom,
        vurdertAv = vurdertAvService.medNavnOgEnhet(vurdertAv, vurdertDato.toLocalDate()),
        besluttetAv = vurdertAvService.besluttetAv(
            definisjon = Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP,
            behandlingId = vurdertIBehandling
        ),
        lovvalg = lovvalg.toResponse(),
        medlemskap = medlemskap?.toResponse(),
        overstyrt = overstyrt
    )

fun MedlemskapDto.toResponse() =
    MedlemskapResponse(
        begrunnelse = begrunnelse,
        varMedlemIFolketrygd = varMedlemIFolketrygd
    )

fun LovvalgDto.toResponse() =
    LovvalgResponse(
        begrunnelse = begrunnelse,
        lovvalgsLandEllerLandMedAvtale = lovvalgsLandEllerLandMedTrygdeAvtale
    )