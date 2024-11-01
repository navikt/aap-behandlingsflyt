package no.nav.aap.behandlingsflyt.faktasaksbehandler.student

import java.time.LocalDate

data class StudentVurdering(
    val id: Long? = null,
    val begrunnelse: String,
    val harAvbruttStudie: Boolean,
    val godkjentStudieAvLånekassen: Boolean?,
    val avbruttPgaSykdomEllerSkade: Boolean?,
    val harBehovForBehandling: Boolean?,
    val avbruttStudieDato: LocalDate?,
    val avbruddMerEnn6Måneder: Boolean?,
) {
    fun erOppfylt(): Boolean {
        return harAvbruttStudie && godkjentStudieAvLånekassen == true && avbruttPgaSykdomEllerSkade == true && harBehovForBehandling == true && avbruddMerEnn6Måneder == true
    }
}
