package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.historiskevurderinger.HistoriskVurderingDto
import no.nav.aap.behandlingsflyt.historiskevurderinger.ÅpenPeriodeDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDate
import java.time.LocalDateTime

data class ManuellVurderingForLovvalgMedlemskap(
    val id: Long? = null,
    val lovvalgVedSøknadsTidspunkt: LovvalgVedSøknadsTidspunktDto,
    val medlemskapVedSøknadsTidspunkt: MedlemskapVedSøknadsTidspunktDto?,
    val vurdertAv: String,
    val vurdertDato: LocalDateTime? = null,
    val overstyrt: Boolean = false,

    // Nye felter for å støtte periodisering - skal gjøres obligatoriske etter migrering
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
    val vurdertIBehandling: BehandlingId? = null,
) {
    fun lovvalgslandErAnnetLandIEØSEllerLandMedAvtale(): Boolean {
        val lovvalgsLand = lovvalgVedSøknadsTidspunkt.lovvalgsEØSLandEllerLandMedAvtale
        return lovvalgsLand != null && lovvalgsLand != EØSLandEllerLandMedAvtale.NOR && lovvalgsLand in enumValues<EØSLandEllerLandMedAvtale>().map { it }
    }

    fun medlemIFolketrygd(): Boolean {
        return medlemskapVedSøknadsTidspunkt?.varMedlemIFolketrygd ?: false
    }
}

data class ManuellVurderingForLovvalgMedlemskapDto(
    val lovvalgVedSøknadsTidspunkt: LovvalgVedSøknadsTidspunktDto,
    val medlemskapVedSøknadsTidspunkt: MedlemskapVedSøknadsTidspunktDto?
)

data class PeriodisertManuellVurderingForLovvalgMedlemskapDto(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val begrunnelse: String,
    val lovvalg: LovvalgVedSøknadsTidspunktDto,
    val medlemskap: MedlemskapVedSøknadsTidspunktDto?,
) : LøsningForPeriode {
    fun toManuellVurderingForLovvalgMedlemskap(
        kontekst: AvklaringsbehovKontekst,
        overstyrt : Boolean,
    ): ManuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskap(
        fom = fom,
        tom = tom,
        vurdertIBehandling = kontekst.behandlingId(),
        lovvalgVedSøknadsTidspunkt = lovvalg,
        medlemskapVedSøknadsTidspunkt = medlemskap,
        vurdertAv = kontekst.bruker.ident,
        vurdertDato = LocalDateTime.now(),
        overstyrt = overstyrt
    )
}

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
    val lovvalgsEØSLandEllerLandMedAvtale: EØSLandEllerLandMedAvtale?,
)

data class MedlemskapVedSøknadsTidspunktDto(
    val begrunnelse: String?,
    val varMedlemIFolketrygd: Boolean?
)

