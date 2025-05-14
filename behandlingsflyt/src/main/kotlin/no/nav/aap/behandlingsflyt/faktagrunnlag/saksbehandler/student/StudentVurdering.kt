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

    fun tilResponse(): StudentVurderingResponse {
        return StudentVurderingResponse(
            id = this.id,
            begrunnelse = this.begrunnelse,
            harAvbruttStudie = this.harAvbruttStudie,
            godkjentStudieAvLånekassen = this.godkjentStudieAvLånekassen,
            avbruttPgaSykdomEllerSkade = this.avbruttPgaSykdomEllerSkade,
            harBehovForBehandling = this.harBehovForBehandling,
            avbruttStudieDato = this.avbruttStudieDato,
            avbruddMerEnn6Måneder = this.avbruddMerEnn6Måneder,
            vurdertAv = VurdertAvResponse(
                this.vurdertAv,
                this.vurdertTidspunkt?.toLocalDate() ?: error("Mangler vurdertdato på studentvurdering")
            )
        )
    }
}

data class StudentVurderingResponse(
    val id: Long? = null,
    val begrunnelse: String,
    val harAvbruttStudie: Boolean,
    val godkjentStudieAvLånekassen: Boolean?,
    val avbruttPgaSykdomEllerSkade: Boolean?,
    val harBehovForBehandling: Boolean?,
    val avbruttStudieDato: LocalDate?,
    val avbruddMerEnn6Måneder: Boolean?,
    val vurdertAv: VurdertAvResponse,
)

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

data class VurdertAvResponse(
    val ident: String,
    val dato: LocalDate
)