package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLand
import no.nav.aap.behandlingsflyt.historiskevurderinger.HistoriskVurderingDto
import no.nav.aap.behandlingsflyt.historiskevurderinger.ÅpenPeriodeDto
import java.time.LocalDate
import java.time.LocalDateTime

data class ManuellVurderingForLovvalgMedlemskap(
    val lovvalgVedSøknadsTidspunkt: LovvalgVedSøknadsTidspunktDto,
    val medlemskapVedSøknadsTidspunkt: MedlemskapVedSøknadsTidspunktDto?,
    val vurdertAv: String,
    val vurdertDato: LocalDateTime? = null,
    val overstyrt: Boolean = false
)

data class ManuellVurderingForLovvalgMedlemskapDto(
    val lovvalgVedSøknadsTidspunkt: LovvalgVedSøknadsTidspunktDto,
    val medlemskapVedSøknadsTidspunkt: MedlemskapVedSøknadsTidspunktDto?
)

class HistoriskManuellVurderingForLovvalgMedlemskap(
    vurdertDato: LocalDate,
    vurdertAvIdent: String,
    erGjeldendeVurdering: Boolean,
    periode: ÅpenPeriodeDto,
    vurdering: ManuellVurderingForLovvalgMedlemskap
) : HistoriskVurderingDto<ManuellVurderingForLovvalgMedlemskap>(
    vurdertDato, vurdertAvIdent, erGjeldendeVurdering, periode, vurdering
)

data class LovvalgVedSøknadsTidspunktDto(
    val begrunnelse: String,
    val lovvalgsEØSLand: EØSLand?,
)

data class MedlemskapVedSøknadsTidspunktDto(
    val begrunnelse: String?,
    val varMedlemIFolketrygd: Boolean?
)

