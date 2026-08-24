package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.enums.enumEntries

data class ManuellVurderingForLovvalgMedlemskap(
    val lovvalg: LovvalgDto,
    val medlemskap: MedlemskapDto?,
    val vurdertAv: Bruker,
    val vurdertDato: LocalDateTime,
    val overstyrt: Boolean = false,
    override val fom: LocalDate,
    override val tom: LocalDate? = null,
    override val vurdertIBehandling: BehandlingId,
) : PeriodisertVurdering {
    override val opprettet: Instant = vurdertDato.atZone(ZoneId.of("Europe/Oslo")).toInstant()

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

    @Deprecated("Bruk begrunnelse i lovvag/medlemskap istedet") override val begrunnelse: String,
    val lovvalg: LovvalgDto,
    val medlemskap: MedlemskapDto?,
) : LøsningForPeriode {
    fun toManuellVurderingForLovvalgMedlemskap(
        kontekst: AvklaringsbehovKontekst,
        overstyrt: Boolean,
    ): ManuellVurderingForLovvalgMedlemskap =
        toManuellVurderingForLovvalgMedlemskap(overstyrt, kontekst.bruker, kontekst.behandlingId())

    fun toManuellVurderingForLovvalgMedlemskap(
        overstyrt: Boolean,
        bruker: Bruker,
        vurdertIBehandling: BehandlingId,
    ): ManuellVurderingForLovvalgMedlemskap = ManuellVurderingForLovvalgMedlemskap(
        fom = fom,
        tom = tom,
        vurdertIBehandling = vurdertIBehandling,
        lovvalg = lovvalg,
        medlemskap = medlemskap,
        vurdertAv = bruker,
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

