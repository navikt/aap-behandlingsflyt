package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattNavnOgEnhet
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.HistoriskManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.historiskevurderinger.HistoriskVurderingDto
import no.nav.aap.behandlingsflyt.historiskevurderinger.ÅpenPeriodeDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class LovvalgMedlemskapGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: ManuellVurderingForLovvalgMedlemskapResponse?,
    val historiskeManuelleVurderinger: List<HistoriskManuellVurderingForLovvalgMedlemskapResponse>
)

data class ManuellVurderingForLovvalgMedlemskapResponse(
    val lovvalgVedSøknadsTidspunkt: LovvalgResponse,
    val medlemskapVedSøknadsTidspunkt: MedlemskapResponse?,
    val vurdertAv: VurdertAvResponse,
    val overstyrt: Boolean = false
)

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
    val lovvalgsEØSLandEllerLandMedAvtale: EØSLandEllerLandMedAvtale?
)

data class MedlemskapResponse(
    val begrunnelse: String?,
    val varMedlemIFolketrygd: Boolean?
)

class HistoriskManuellVurderingForLovvalgMedlemskapResponse(
    vurdertDato: LocalDate,
    vurdertAvIdent: String,
    erGjeldendeVurdering: Boolean,
    periode: ÅpenPeriodeDto,
    vurdering: ManuellVurderingForLovvalgMedlemskapResponse
) : HistoriskVurderingDto<ManuellVurderingForLovvalgMedlemskapResponse>(
        vurdertDato,
        vurdertAvIdent,
        erGjeldendeVurdering,
        periode,
        vurdering
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
        lovvalg = lovvalgVedSøknadsTidspunkt.toResponse(),
        medlemskap = medlemskapVedSøknadsTidspunkt?.toResponse(),
        overstyrt = overstyrt
    )

fun ManuellVurderingForLovvalgMedlemskap.toResponse(ansattNavnOgEnhet: AnsattNavnOgEnhet?) =
    ManuellVurderingForLovvalgMedlemskapResponse(
        lovvalgVedSøknadsTidspunkt = lovvalgVedSøknadsTidspunkt.toResponse(),
        medlemskapVedSøknadsTidspunkt = medlemskapVedSøknadsTidspunkt?.toResponse(),
        vurdertAv =
            VurdertAvResponse(
                ident = vurdertAv,
                dato = vurdertDato.toLocalDate(),
                ansattnavn = ansattNavnOgEnhet?.navn,
                enhetsnavn = ansattNavnOgEnhet?.enhet
            ),
        overstyrt = overstyrt
    )

fun MedlemskapVedSøknadsTidspunktDto.toResponse() =
    MedlemskapResponse(
        begrunnelse = begrunnelse,
        varMedlemIFolketrygd = varMedlemIFolketrygd
    )

fun LovvalgVedSøknadsTidspunktDto.toResponse() =
    LovvalgResponse(
        begrunnelse = begrunnelse,
        lovvalgsEØSLandEllerLandMedAvtale = lovvalgsEØSLandEllerLandMedAvtale
    )

fun HistoriskManuellVurderingForLovvalgMedlemskap.toResponse() =
    HistoriskManuellVurderingForLovvalgMedlemskapResponse(
        vurdertDato = vurdertDato,
        vurdertAvIdent = vurdertAvIdent,
        erGjeldendeVurdering = erGjeldendeVurdering,
        periode = periode,
        vurdering = vurdering.toResponse(ansattNavnOgEnhet = null)
    )