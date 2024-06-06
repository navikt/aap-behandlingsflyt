package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate
import java.time.Year

class BeregningService(
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val sykdomRepository: SykdomRepository,
    private val uføreRepository: UføreRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository
) {

    fun beregnGrunnlag(behandlingId: BehandlingId): Beregningsgrunnlag {
        val inntektGrunnlag = inntektGrunnlagRepository.hent(behandlingId)
        val sykdomGrunnlag = sykdomRepository.hent(behandlingId)
        val uføre = uføreRepository.hentHvisEksisterer(behandlingId)
        val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)

        val input = utledInput(
            sykdomGrunnlag.sykdomsvurdering!!,
            sykdomGrunnlag.yrkesskadevurdering,
            beregningVurdering,
            inntektGrunnlag.inntekter,
            uføre?.vurdering?.uføregrad
        )

        val beregningMedEllerUtenUføreMedEllerUtenYrkesskade = beregneMedInput(input)

        beregningsgrunnlagRepository.lagre(behandlingId, beregningMedEllerUtenUføreMedEllerUtenYrkesskade)
        return beregningMedEllerUtenUføreMedEllerUtenYrkesskade
    }

    private fun beregneMedInput(input: Inntektsbehov): Beregningsgrunnlag {
        val grunnlag11_19 = beregn(input.utledForOrdinær())//6G begrensning ligger her samt gjennomsnitt

        val beregningMedEllerUtenUføre = if (input.skalBeregneMedUføre()) {
            val beregningVedUføre = beregn(input.utledForYtterligereNedsatt())// år kommer herfra //6G begrensning ligger her samt gjennomsnitt
            val uføreberegning = UføreBeregning(
                grunnlag = grunnlag11_19,
                ytterligereNedsattGrunnlag = beregningVedUføre,
                //TODO:
                // Hva hvis bruker har flere uføregrader?
                // Skal saksbahandler velge den som er knyttet til ytterligere nedsatt-tidspunktet?
                uføregrad = input.uføregrad()
            )
            val grunnlagUføre = uføreberegning.beregnUføre(Year.from(input.hentYtterligereNedsattArbeidsevneDato()))
            grunnlagUføre
        } else {
            grunnlag11_19
        }

        val beregningMedEllerUtenUføreMedEllerUtenYrkesskade =
            if (input.skalBeregneMedYrkesskadeFordel()) { //11-22
                val inntektPerÅr = InntektPerÅr(
                    Year.from(input.skadetidspunkt()),
                    input.antattÅrligInntekt()
                )
                val yrkesskaden = YrkesskadeBeregning(
                    grunnlag11_19 = beregningMedEllerUtenUføre,
                    antattÅrligInntekt = inntektPerÅr,
                    andelAvNedsettelsenSomSkyldesYrkesskaden = input.andelYrkesskade()
                ).beregnYrkesskaden()
                yrkesskaden
            } else {
                beregningMedEllerUtenUføre
            }
        return beregningMedEllerUtenUføreMedEllerUtenYrkesskade
    }

    private fun utledInput(
        sykdomsvurdering: Sykdomsvurdering,
        yrkesskadevurdering: Yrkesskadevurdering?,
        vurdering: BeregningVurdering?,
        inntekter: Set<InntektPerÅr>,
        uføregrad: Prosent?
    ): Inntektsbehov {
        return Inntektsbehov(
            Input(
                nedsettelsesDato = requireNotNull(sykdomsvurdering.nedsattArbeidsevneDato?.let { LocalDate.of(it, 1, 1) }),
                inntekter = inntekter,
                uføregrad = uføregrad,
                yrkesskadevurdering = yrkesskadevurdering,
                beregningVurdering = vurdering
            )
        )
    }

    private fun beregn(
        inntekterPerÅr: Set<InntektPerÅr>
    ): Beregningsgrunnlag {
        val grunnlag11_19 =
            GrunnlagetForBeregningen(inntekterPerÅr).beregnGrunnlaget()

        return grunnlag11_19
    }
}
