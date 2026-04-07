package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Diagnose
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate
import java.time.LocalDateTime

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
    val vurdertAv: String,
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

data class PeriodisertStudentDto(
    override val fom: LocalDate,
    override val tom: LocalDate? = null,
    override val begrunnelse: String,
    val harAvbruttStudie: Boolean,
    val godkjentStudieAvLånekassen: Boolean?,
    val avbruttPgaSykdomEllerSkade: Boolean?,
    val harBehovForBehandling: Boolean?,
    val avbruttStudieDato: LocalDate?,
    val avbruddMerEnn6Måneder: Boolean?,
    val kodeverk: String? = null,
    val hoveddiagnose: String? = null,
    val bidiagnoser: List<String>? = emptyList(),
) : LøsningForPeriode {
    fun tilStudentVurdering(bruker: Bruker, vurdertIBehandling: BehandlingId): StudentVurdering {
        return StudentVurdering(
            fom = fom,
            tom = tom,
            begrunnelse = begrunnelse,
            harAvbruttStudie = harAvbruttStudie,
            godkjentStudieAvLånekassen = godkjentStudieAvLånekassen,
            avbruttPgaSykdomEllerSkade = avbruttPgaSykdomEllerSkade,
            harBehovForBehandling = harBehovForBehandling,
            avbruttStudieDato = avbruttStudieDato,
            avbruddMerEnn6Måneder = avbruddMerEnn6Måneder,
            vurdertAv = bruker.ident,
            vurdertTidspunkt = LocalDateTime.now(),
            vurdertIBehandling = vurdertIBehandling,
            diagnose = kodeverk?.let {
                Diagnose(
                    kodeverk = it,
                    hoveddiagnose = hoveddiagnose,
                    bidiagnoser = bidiagnoser
                )
            }
        )
    }

    fun tilGammelDto(): StudentVurderingDTO {
        return StudentVurderingDTO(
            fom = fom,
            tom = tom,
            begrunnelse = begrunnelse,
            harAvbruttStudie = harAvbruttStudie,
            godkjentStudieAvLånekassen = godkjentStudieAvLånekassen,
            avbruttPgaSykdomEllerSkade = avbruttPgaSykdomEllerSkade,
            harBehovForBehandling = harBehovForBehandling,
            avbruttStudieDato = avbruttStudieDato,
            avbruddMerEnn6Måneder = avbruddMerEnn6Måneder
        )
    }
}

data class StudentVurderingDTO(
    val fom: LocalDate? = null, // TODO: Gjør denne påkrevd
    val tom: LocalDate? = null,
    val begrunnelse: String,
    val harAvbruttStudie: Boolean,
    val godkjentStudieAvLånekassen: Boolean?,
    val avbruttPgaSykdomEllerSkade: Boolean?,
    val harBehovForBehandling: Boolean?,
    val avbruttStudieDato: LocalDate?,
    val avbruddMerEnn6Måneder: Boolean?,
    val kodeverk: String? = null,
    val hoveddiagnose: String? = null,
    val bidiagnoser: List<String>? = emptyList(),
) {
    fun tilStudentVurdering(bruker: Bruker, vurdertIBehandling: BehandlingId, defaultFom: LocalDate): StudentVurdering {
        return StudentVurdering(
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
            vurdertTidspunkt = LocalDateTime.now(),
            vurdertIBehandling = vurdertIBehandling,
            diagnose = kodeverk?.let {
                Diagnose(
                    kodeverk = it,
                    hoveddiagnose = hoveddiagnose,
                    bidiagnoser = bidiagnoser
                )
            }
        )
    }
}
