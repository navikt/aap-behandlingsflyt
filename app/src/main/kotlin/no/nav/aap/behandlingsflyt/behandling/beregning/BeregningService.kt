package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.faktasaksbehandler.student.StudentVurdering
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate

class BeregningService(
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val sykdomRepository: SykdomRepository,
    private val studentRepository: StudentRepository,
    private val uføreRepository: UføreRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository
) {

    fun beregnGrunnlag(behandlingId: BehandlingId): Beregningsgrunnlag {
        val inntektGrunnlag = inntektGrunnlagRepository.hent(behandlingId)
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
        val uføre = uføreRepository.hentHvisEksisterer(behandlingId)
        val student = studentRepository.hentHvisEksisterer(behandlingId)
        val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)

        val input = utledInput(
            sykdomsvurdering = sykdomGrunnlag?.sykdomsvurdering,
            studentVurdering = student?.studentvurdering,
            yrkesskadevurdering = sykdomGrunnlag?.yrkesskadevurdering,
            vurdering = beregningVurdering,
            inntekter = inntektGrunnlag.inntekter,
            uføregrad = uføre?.vurdering?.uføregrad
        )

        val beregning = Beregning(input)
        val beregningMedEllerUtenUføreMedEllerUtenYrkesskade = beregning.beregneMedInput()

        beregningsgrunnlagRepository.lagre(behandlingId, beregningMedEllerUtenUføreMedEllerUtenYrkesskade)
        return beregningMedEllerUtenUføreMedEllerUtenYrkesskade
    }

    fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        beregningsgrunnlagRepository.deaktiver(behandlingId)
    }

    private fun utledInput(
        sykdomsvurdering: Sykdomsvurdering?,
        studentVurdering: StudentVurdering?,
        yrkesskadevurdering: Yrkesskadevurdering?,
        vurdering: BeregningVurdering?,
        inntekter: Set<InntektPerÅr>,
        uføregrad: Prosent?
    ): Inntektsbehov {
        return Inntektsbehov(
            Input(
                nedsettelsesDato = utledNedsettelsesdato(sykdomsvurdering, studentVurdering),
                inntekter = inntekter,
                uføregrad = uføregrad,
                yrkesskadevurdering = yrkesskadevurdering,
                beregningVurdering = vurdering
            )
        )
    }

    private fun utledNedsettelsesdato(
        sykdomsvurdering: Sykdomsvurdering?,
        studentVurdering: StudentVurdering?
    ): LocalDate {
        val nedsettelsesdatoer = setOf(
            sykdomsvurdering?.nedsattArbeidsevneDato,
            studentVurdering?.avbruttStudieDato
        ).filterNotNull()

        return nedsettelsesdatoer.min()
    }

}
