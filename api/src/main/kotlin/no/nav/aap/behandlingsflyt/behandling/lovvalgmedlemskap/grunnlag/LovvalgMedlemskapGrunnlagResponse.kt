package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattNavnOgEnhet
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.HistoriskManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.historiskevurderinger.HistoriskVurderingDto
import no.nav.aap.behandlingsflyt.historiskevurderinger.ÅpenPeriodeDto
import java.time.LocalDate

data class LovvalgMedlemskapGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: ManuellVurderingForLovvalgMedlemskapResponse?,
    val historiskeManuelleVurderinger: List<HistoriskManuellVurderingForLovvalgMedlemskapResponse>
)

data class ManuellVurderingForLovvalgMedlemskapResponse(
    val lovvalgVedSøknadsTidspunkt: LovvalgVedSøknadsTidspunktResponse,
    val medlemskapVedSøknadsTidspunkt: MedlemskapVedSøknadsTidspunktResponse?,
    val vurdertAv: VurdertAvResponse,
    val overstyrt: Boolean = false
)

data class LovvalgVedSøknadsTidspunktResponse(
    val begrunnelse: String,
    val lovvalgsEØSLandEllerLandMedAvtale: EØSLandEllerLandMedAvtale?
)

data class MedlemskapVedSøknadsTidspunktResponse(
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

fun ManuellVurderingForLovvalgMedlemskap.toResponse(ansattNavnOgEnhet: AnsattNavnOgEnhet?) =
    ManuellVurderingForLovvalgMedlemskapResponse(
        lovvalgVedSøknadsTidspunkt = lovvalgVedSøknadsTidspunkt.toResponse(),
        medlemskapVedSøknadsTidspunkt = medlemskapVedSøknadsTidspunkt?.toResponse(),
        vurdertAv =
            VurdertAvResponse(
                ident = vurdertAv,
                dato =
                    vurdertDato?.toLocalDate()
                        ?: error("Mangler vurdertDato på ManuellVurderingForLovvalgMedlemskap"),
                ansattnavn = ansattNavnOgEnhet?.navn,
                enhetsnavn = ansattNavnOgEnhet?.enhet
            ),
        overstyrt = overstyrt
    )

private fun MedlemskapVedSøknadsTidspunktDto.toResponse() =
    MedlemskapVedSøknadsTidspunktResponse(
        begrunnelse = begrunnelse,
        varMedlemIFolketrygd = varMedlemIFolketrygd
    )

private fun LovvalgVedSøknadsTidspunktDto.toResponse() =
    LovvalgVedSøknadsTidspunktResponse(
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