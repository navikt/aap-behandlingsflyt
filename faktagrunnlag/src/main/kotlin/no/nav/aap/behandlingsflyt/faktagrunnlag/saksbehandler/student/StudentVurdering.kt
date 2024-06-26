package no.nav.aap.behandlingsflyt.faktasaksbehandler.student

import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class StudentVurdering(
    val id: Long? = null,
    val begrunnelse: String,
    val harAvbruttStudie: Boolean,
    val godkjentStudieAvLånekassen: Boolean,
    val avbruttPgaSykdomEllerSkade: Boolean,
    val harBehovForBehandling: Boolean,
    val skalGjenopptaStudie: Boolean,
    val avbruttStudieDato: LocalDate,
    val avbruddMerEnn6Måneder: Boolean,
    val dokumenterBruktIVurdering: List<JournalpostId>,
){
        fun erOppfylt(): Boolean {
        return harAvbruttStudie && godkjentStudieAvLånekassen && avbruttPgaSykdomEllerSkade && harBehovForBehandling && skalGjenopptaStudie && avbruddMerEnn6Måneder
    }
}
