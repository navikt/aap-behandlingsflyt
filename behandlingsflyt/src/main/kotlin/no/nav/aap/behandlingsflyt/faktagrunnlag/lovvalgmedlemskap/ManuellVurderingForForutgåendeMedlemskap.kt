package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class ManuellVurderingForForutgåendeMedlemskap(
    val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val varMedlemMedNedsattArbeidsevne: Boolean?,
    val medlemMedUnntakAvMaksFemAar: Boolean?,
    override val vurdertAv: Bruker,
    val vurdertTidspunkt: LocalDateTime,
    val overstyrt: Boolean = false,
    override val vurdertIBehandling: BehandlingId,
    override val fom: LocalDate,
    override val tom: LocalDate? = null
) : PeriodisertVurdering {
    override val opprettet: Instant = vurdertTidspunkt.atZone(ZoneId.of("Europe/Oslo")).toInstant()

    // NB! Denne tar ikke høyde for yrkesskade
    fun oppfyllerForutgåendeMedlemskap(): Boolean {
        return harForutgåendeMedlemskap
                || varMedlemMedNedsattArbeidsevne == true
                || medlemMedUnntakAvMaksFemAar == true
    }
}

data class PeriodisertManuellVurderingForForutgåendeMedlemskapDto(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val varMedlemMedNedsattArbeidsevne: Boolean?,
    val medlemMedUnntakAvMaksFemAar: Boolean?
) : LøsningForPeriode {
    fun toManuellVurderingForForutgåendeMedlemskap(
        kontekst: AvklaringsbehovKontekst,
        overstyrt: Boolean,
    ): ManuellVurderingForForutgåendeMedlemskap =
        toManuellVurderingForForutgåendeMedlemskap(overstyrt, kontekst.bruker, kontekst.behandlingId())

    fun toManuellVurderingForForutgåendeMedlemskap(
        overstyrt: Boolean,
        bruker: Bruker,
        vurdertIBehandling: BehandlingId,
    ): ManuellVurderingForForutgåendeMedlemskap = ManuellVurderingForForutgåendeMedlemskap(
        fom = fom,
        tom = tom,
        vurdertIBehandling = vurdertIBehandling,
        begrunnelse = begrunnelse,
        harForutgåendeMedlemskap = harForutgåendeMedlemskap,
        varMedlemMedNedsattArbeidsevne = varMedlemMedNedsattArbeidsevne,
        medlemMedUnntakAvMaksFemAar = medlemMedUnntakAvMaksFemAar,
        vurdertAv = bruker,
        vurdertTidspunkt = LocalDateTime.now(),
        overstyrt = overstyrt
    )
}

fun List<PeriodisertManuellVurderingForForutgåendeMedlemskapDto>.validerGyldigVurderinger(): Validation<List<PeriodisertManuellVurderingForForutgåendeMedlemskapDto>> {
    forEach {
        val periode = if (it.tom != null) "${it.fom} - ${it.tom}" else "${it.fom}"
        if (it.begrunnelse.isBlank()) {
            return Validation.Invalid(this, "Det mangler begrunnelse for vurdering [$periode]")
        }
        if (it.harForutgåendeMedlemskap && (it.medlemMedUnntakAvMaksFemAar == true || it.varMedlemMedNedsattArbeidsevne == true)) {
            return Validation.Invalid(this, "Kan ikke oppfylle både hovedvilår og unntaksvilkår [$periode]")
        }
    }

    return Validation.Valid(this)
}