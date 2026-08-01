package no.nav.aap.lovvalgmedlemskap

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker

data class ManuellVurderingForForutgåendeMedlemskap(
    val begrunnelse: String,
    val harForutgåendeMedlemskap: Boolean,
    val varMedlemMedNedsattArbeidsevne: Boolean?,
    val medlemMedUnntakAvMaksFemAar: Boolean?,
    val vurdertAv: Bruker,
    val vurdertTidspunkt: LocalDateTime,
    val overstyrt: Boolean = false,
    val vurdertIBehandling: BehandlingId,
    val fom: LocalDate,
    val tom: LocalDate? = null
) {
    // NB! Denne tar ikke høyde for yrkesskade
    fun oppfyllerForutgåendeMedlemskap(): Boolean {
        return harForutgåendeMedlemskap
                || varMedlemMedNedsattArbeidsevne == true
                || medlemMedUnntakAvMaksFemAar == true
    }
}