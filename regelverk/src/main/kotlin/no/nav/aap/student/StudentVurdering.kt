package no.nav.aap.student

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.sykdom.Diagnose

data class StudentVurdering(
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val begrunnelse: String,
    val harAvbruttStudie: Boolean,
    val godkjentStudieAvLånekassen: Boolean?,
    val avbruttPgaSykdomEllerSkade: Boolean?,
    val harBehovForBehandling: Boolean?,
    val avbruttStudieDato: LocalDate?,
    val avbruddMerEnn6Måneder: Boolean?,
    val vurdertAv: Bruker,
    val vurdertTidspunkt: LocalDateTime = LocalDateTime.now(),
    val vurdertIBehandling: BehandlingId,
    val diagnose: Diagnose?
) {
    fun erOppfylt(): Boolean {
        return harAvbruttStudie &&
                godkjentStudieAvLånekassen == true &&
                avbruttPgaSykdomEllerSkade == true &&
                harBehovForBehandling == true &&
                avbruddMerEnn6Måneder == true
    }
}