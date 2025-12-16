package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate
import java.time.LocalDateTime

data class StudentVurdering(
    val id: Long? = null,
    val fom: LocalDate? = null, // TODO: Gjør påkrevd,
    val tom: LocalDate? = null,
    val begrunnelse: String,
    val harAvbruttStudie: Boolean,
    val godkjentStudieAvLånekassen: Boolean?,
    val avbruttPgaSykdomEllerSkade: Boolean?,
    val harBehovForBehandling: Boolean?,
    val avbruttStudieDato: LocalDate?,
    val avbruddMerEnn6Måneder: Boolean?,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null,
    val vurdertIBehandling: BehandlingId?, // TODO: Gjør påkrevd
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
    val fom: LocalDate? = null, // TODO: Gjør denne påkrevd
    val tom: LocalDate? = null,
    val begrunnelse: String,
    val harAvbruttStudie: Boolean,
    val godkjentStudieAvLånekassen: Boolean?,
    val avbruttPgaSykdomEllerSkade: Boolean?,
    val harBehovForBehandling: Boolean?,
    val avbruttStudieDato: LocalDate?,
    val avbruddMerEnn6Måneder: Boolean?,
) {
    fun tilStudentVurdering(bruker: Bruker, vurdertIBehandling: BehandlingId, defaultFom: LocalDate): StudentVurdering {
        return StudentVurdering(
            id = id,
            fom = fom ?: defaultFom,
            tom = tom,
            begrunnelse = begrunnelse,
            harAvbruttStudie = harAvbruttStudie,
            godkjentStudieAvLånekassen = godkjentStudieAvLånekassen,
            avbruttPgaSykdomEllerSkade = avbruttPgaSykdomEllerSkade,
            harBehovForBehandling = harBehovForBehandling,
            avbruttStudieDato = avbruttStudieDato,
            avbruddMerEnn6Måneder = avbruddMerEnn6Måneder,
            vurdertAv = bruker.ident,
            vurdertIBehandling = vurdertIBehandling
        )
    }
}
