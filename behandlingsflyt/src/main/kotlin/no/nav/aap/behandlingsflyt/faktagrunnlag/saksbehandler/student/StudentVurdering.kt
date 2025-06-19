package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import java.time.LocalDate
import java.time.LocalDateTime

data class StudentVurdering(
    val id: Long? = null,
    val begrunnelse: String,
    val harAvbruttStudie: Boolean,
    val godkjentStudieAvLånekassen: Boolean?,
    val avbruttPgaSykdomEllerSkade: Boolean?,
    val harBehovForBehandling: Boolean?,
    val avbruttStudieDato: LocalDate?,
    val avbruddMerEnn6Måneder: Boolean?,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null,
) {
    fun erOppfylt(): Boolean {
        return harAvbruttStudie &&
                godkjentStudieAvLånekassen == true &&
                avbruttPgaSykdomEllerSkade == true &&
                harBehovForBehandling == true &&
                avbruddMerEnn6Måneder == true
    }
}

data class StudentVurderingDTO(
    val id: Long? = null,
    val begrunnelse: String,
    val harAvbruttStudie: Boolean,
    val godkjentStudieAvLånekassen: Boolean?,
    val avbruttPgaSykdomEllerSkade: Boolean?,
    val harBehovForBehandling: Boolean?,
    val avbruttStudieDato: LocalDate?,
    val avbruddMerEnn6Måneder: Boolean?,
)
