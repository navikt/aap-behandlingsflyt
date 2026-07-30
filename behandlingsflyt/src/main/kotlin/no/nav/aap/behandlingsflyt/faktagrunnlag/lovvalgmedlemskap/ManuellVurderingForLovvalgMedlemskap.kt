package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.enums.enumEntries

data class ManuellVurderingForLovvalgMedlemskap(
    val lovvalg: LovvalgDto,
    val medlemskap: MedlemskapDto?,
    val vurdertAv: Bruker,
    val vurdertDato: LocalDateTime,
    val overstyrt: Boolean = false,
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val vurdertIBehandling: BehandlingId,
) {
    fun lovvalgslandErAnnetLandIEØSEllerLandMedAvtale(): Boolean {
        val lovvalgsLand = lovvalg.lovvalgsEØSLandEllerLandMedAvtale
        return lovvalgsLand != EØSLandEllerLandMedAvtale.NOR && lovvalgsLand in enumEntries<EØSLandEllerLandMedAvtale>().map { it }
    }

    fun medlemIFolketrygd(): Boolean {
        return medlemskap?.varMedlemIFolketrygd ?: false
    }
}

data class PeriodisertManuellVurderingForLovvalgMedlemskapDto(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val begrunnelse: String,
    val lovvalg: LovvalgDto,
    val medlemskap: MedlemskapDto?,
) : LøsningForPeriode {
    fun toManuellVurderingForLovvalgMedlemskap(
        kontekst: AvklaringsbehovKontekst,
        overstyrt : Boolean,
    ): ManuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskap(
        fom = fom,
        tom = tom,
        vurdertIBehandling = kontekst.behandlingId(),
        lovvalg = lovvalg,
        medlemskap = medlemskap,
        vurdertAv = kontekst.bruker,
        vurdertDato = LocalDateTime.now(),
        overstyrt = overstyrt
    )
}

data class LovvalgDto(
    val begrunnelse: String,
    val lovvalgsEØSLandEllerLandMedAvtale: EØSLandEllerLandMedAvtale,
)

data class MedlemskapDto(
    val begrunnelse: String,
    val varMedlemIFolketrygd: Boolean
)

