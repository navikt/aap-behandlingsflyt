package no.nav.aap.behandlingsflyt.behandling.student

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

open class StudentGrunnlagResponse(
    val oppgittStudent: OppgittStudent?,
    override val harTilgangTilÅSaksbehandle: Boolean,
    override val nyeVurderinger: List<StudentVurderingResponse>,
    override val sisteVedtatteVurderinger: List<StudentVurderingResponse>,
    override val kanVurderes: List<Periode>,
    override val behøverVurderinger: List<Periode>,
    @Deprecated("Bruk nyeVurderinger")
    val studentvurdering: StudentVurderingResponse?,
) : PeriodiserteVurderingerDto<StudentVurderingResponse>

data class StudentVurderingResponse(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertAv: VurdertAvResponse,
    override val kvalitetssikretAv: VurdertAvResponse? = null,
    override val besluttetAv: VurdertAvResponse?,

    val begrunnelse: String,
    val harAvbruttStudie: Boolean,
    val godkjentStudieAvLånekassen: Boolean?,
    val avbruttPgaSykdomEllerSkade: Boolean?,
    val harBehovForBehandling: Boolean?,
    val avbruttStudieDato: LocalDate?,
    val avbruddMerEnn6Måneder: Boolean?,
) : VurderingDto {
    companion object {
        fun fraDomene(
            tidslinje: Tidslinje<StudentVurdering>,
            vurdertAvService: VurdertAvService
        ): List<StudentVurderingResponse> {
            val segmenter = tidslinje.segmenter().toList()
            return segmenter
                .mapIndexed { index, segment ->
                    fraDomene(
                        studentVurdering = segment.verdi,
                        vurdertAvService = vurdertAvService,
                        fom = segment.fom(),
                        tom = segment.tom()
                    )
                }
        }

        fun fraDomene(
            studentVurdering: StudentVurdering,
            vurdertAvService: VurdertAvService,
            fom: LocalDate = studentVurdering.fom,
            tom: LocalDate? = studentVurdering.tom
        ): StudentVurderingResponse {
            return StudentVurderingResponse(
                fom = fom,
                tom = tom,
                vurdertAv = vurdertAvService.medNavnOgEnhet(
                    studentVurdering.vurdertAv,
                    studentVurdering.vurdertTidspunkt
                ),
                kvalitetssikretAv = null,
                besluttetAv = vurdertAvService.besluttetAv(
                    definisjon = Definisjon.AVKLAR_STUDENT,
                    behandlingId = studentVurdering.vurdertIBehandling,
                ),
                begrunnelse = studentVurdering.begrunnelse,
                harAvbruttStudie = studentVurdering.harAvbruttStudie,
                godkjentStudieAvLånekassen = studentVurdering.godkjentStudieAvLånekassen,
                avbruttPgaSykdomEllerSkade = studentVurdering.avbruttPgaSykdomEllerSkade,
                harBehovForBehandling = studentVurdering.harBehovForBehandling,
                avbruttStudieDato = studentVurdering.avbruttStudieDato,
                avbruddMerEnn6Måneder = studentVurdering.avbruddMerEnn6Måneder
            )
        }
    }
}

