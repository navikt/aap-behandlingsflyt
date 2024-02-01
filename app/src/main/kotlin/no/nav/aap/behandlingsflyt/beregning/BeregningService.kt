package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.behandlingsflyt.faktagrunnlag.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.beregning.GrunnlagetForBeregningen
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.uføre.UføreRepository
import java.time.Year

class BeregningService(
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val sykdomRepository: SykdomRepository,
    private val uføreRepository: UføreRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository
) {

    fun beregnGrunnlag(behandlingId: BehandlingId): Beregningsgrunnlag {
        val inntektGrunnlag = inntektGrunnlagRepository.hent(behandlingId)
        val sykdomGrunnlag = sykdomRepository.hent(behandlingId)
        val uføre = uføreRepository.hentHvisEksisterer(behandlingId)

        val inntekter = sykdomGrunnlag.utledInput()

        val beregningMedYrkesskade = beregn(sykdomGrunnlag, inntekter.utledForOrdinær(inntektGrunnlag.inntekter))

        val inntekterYtterligereNedsatt = inntekter.utledForYtterligereNedsatt(inntektGrunnlag.inntekter)

        val uføregrad = uføre?.vurdering?.uføregrad

        if (inntekterYtterligereNedsatt != null && uføregrad != null) {
            val beregningMedYrkesskadeVedYtterligereNedsatt = beregn(sykdomGrunnlag, inntekterYtterligereNedsatt)
            val uføreberegning = UføreBeregning(
                beregningMedYrkesskade,
                beregningMedYrkesskadeVedYtterligereNedsatt,
                //TODO:
                // Hva hvis bruker har flere uføregrader?
                // Skal saksbahandler velge den som er knyttet til ytterligere nedsatt-tidspunktet?
                uføregrad = uføregrad
            )
            val grunnlagUføre = uføreberegning.beregnUføre()
            beregningsgrunnlagRepository.lagre(behandlingId, grunnlagUføre)
            return grunnlagUføre
        }

        beregningsgrunnlagRepository.lagre(behandlingId, beregningMedYrkesskade)

        return beregningMedYrkesskade
    }

    private fun beregn(
        sykdomGrunnlag: SykdomGrunnlag,
        inntekterPerÅr: Set<InntektPerÅr>
    ): Beregningsgrunnlag {
        val grunnlag11_19 =
            GrunnlagetForBeregningen(inntekterPerÅr).beregnGrunnlaget()

        val skadetidspunkt = sykdomGrunnlag.yrkesskadevurdering?.skadetidspunkt
        val antattÅrligInntekt = sykdomGrunnlag.yrkesskadevurdering?.antattÅrligInntekt
        val andelAvNedsettelsenSomSkyldesYrkesskaden = sykdomGrunnlag.yrkesskadevurdering?.andelAvNedsettelse
        if (skadetidspunkt != null && antattÅrligInntekt != null && andelAvNedsettelsenSomSkyldesYrkesskaden != null) {
            val inntektPerÅr = InntektPerÅr(Year.from(skadetidspunkt), antattÅrligInntekt)
            val yrkesskaden = YrkesskadeBeregning(
                grunnlag11_19 = grunnlag11_19,
                antattÅrligInntekt = inntektPerÅr,
                andelAvNedsettelsenSomSkyldesYrkesskaden = andelAvNedsettelsenSomSkyldesYrkesskaden
            ).beregnYrkesskaden()
            return yrkesskaden
        }

        return grunnlag11_19
    }
}
