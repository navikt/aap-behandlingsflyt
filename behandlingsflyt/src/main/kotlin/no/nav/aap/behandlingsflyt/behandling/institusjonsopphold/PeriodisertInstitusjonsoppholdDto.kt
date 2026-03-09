package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import java.time.LocalDate

data class PeriodisertInstitusjonsoppholdDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val begrunnelse: String,
    val faarFriKostOgLosji: Boolean,
    val forsoergerEktefelle: Boolean?,
    val harFasteUtgifter: Boolean?,
): LøsningForPeriode