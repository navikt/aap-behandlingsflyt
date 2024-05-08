package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.verdityper.Beløp
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

        val inntekter = utledInput(sykdomGrunnlag.sykdomsvurdering!!, beregningVurdering)

        val grunnlag11_19 = beregn(inntekter.utledForOrdinær(inntektGrunnlag.inntekter))

        val inntekterYtterligereNedsatt = inntekter.utledForYtterligereNedsatt(inntektGrunnlag.inntekter)

        val uføregrad = uføre?.vurdering?.uføregrad

        val beregningMedEllerUtenUføre = if (skalBeregnemedUføre(inntekterYtterligereNedsatt,uføregrad)) {
            val beregningVedUføre = beregn(inntekterYtterligereNedsatt!!)
            val uføreberegning = UføreBeregning(
                grunnlag = grunnlag11_19,
                ytterligereNedsattGrunnlag = beregningVedUføre,
                //TODO:
                // Hva hvis bruker har flere uføregrader?
                // Skal saksbahandler velge den som er knyttet til ytterligere nedsatt-tidspunktet?
                uføregrad = uføregrad!!
            )
            val grunnlagUføre = uføreberegning.beregnUføre()
            grunnlagUføre
        } else {
            grunnlag11_19
        }

        val skadetidspunkt = sykdomGrunnlag.yrkesskadevurdering?.skadetidspunkt
        val antattÅrligInntekt = beregningVurdering?.antattÅrligInntekt
        val andelAvNedsettelsenSomSkyldesYrkesskaden = sykdomGrunnlag.yrkesskadevurdering?.andelAvNedsettelse

        val beregningMedEllerUtenUføreMedEllerUtenYrkesskade =
            if (skalBeregneMedYrkesskadeFordel(skadetidspunkt,antattÅrligInntekt,andelAvNedsettelsenSomSkyldesYrkesskaden)) { //11-22
                val inntektPerÅr = InntektPerÅr(
                    Year.from(skadetidspunkt),
                    antattÅrligInntekt!!
                )
                val yrkesskaden = YrkesskadeBeregning(
                    grunnlag11_19 = beregningMedEllerUtenUføre as Grunnlag11_19,
                    antattÅrligInntekt = inntektPerÅr,
                    andelAvNedsettelsenSomSkyldesYrkesskaden = andelAvNedsettelsenSomSkyldesYrkesskaden!!
                ).beregnYrkesskaden()
                yrkesskaden
            } else {
                beregningMedEllerUtenUføre
            }

        beregningsgrunnlagRepository.lagre(behandlingId, beregningMedEllerUtenUføreMedEllerUtenYrkesskade)
        return beregningMedEllerUtenUføreMedEllerUtenYrkesskade
    }

    private fun utledInput(sykdomsvurdering: Sykdomsvurdering, vurdering: BeregningVurdering?): Inntektsbehov {
        return Inntektsbehov(
            Input(
                nedsettelsesDato = requireNotNull(sykdomsvurdering.nedsattArbeidsevneDato).atMonth(1).atDay(1),
                ytterligereNedsettelsesDato = vurdering?.ytterligereNedsattArbeidsevneDato
            )
        )
    }

    private fun skalBeregnemedUføre(inntekterYtterligereNedsatt: Set<InntektPerÅr>?, uføregrad: Prosent?): Boolean {
        return inntekterYtterligereNedsatt != null && uføregrad != null
    }

    private fun skalBeregneMedYrkesskadeFordel(
        skadetidspunkt: LocalDate?,
        antattÅrligInntekt: Beløp?,
        andelAvNedsettelsenSomSkyldesYrkesskaden: Prosent?
    ): Boolean {
        return skadetidspunkt != null && antattÅrligInntekt != null && andelAvNedsettelsenSomSkyldesYrkesskaden != null
    }

    private fun beregn(
        inntekterPerÅr: Set<InntektPerÅr>
    ): Beregningsgrunnlag {
        val grunnlag11_19 =
            GrunnlagetForBeregningen(inntekterPerÅr).beregnGrunnlaget()

        return grunnlag11_19
    }
}
