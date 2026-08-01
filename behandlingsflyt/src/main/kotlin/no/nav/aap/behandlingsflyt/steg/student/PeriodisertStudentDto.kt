package no.nav.aap.behandlingsflyt.steg.student

import no.nav.aap.behandling.BehandlingId
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.student.StudentVurdering
import no.nav.aap.sykdom.Diagnose

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
            vurdertAv = bruker,
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