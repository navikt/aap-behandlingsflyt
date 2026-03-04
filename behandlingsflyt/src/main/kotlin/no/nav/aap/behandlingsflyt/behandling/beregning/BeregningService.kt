package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Year

class BeregningService(
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val sykdomRepository: SykdomRepository,
    private val uføreRepository: UføreRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository
) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        inntektGrunnlagRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        uføreRepository = repositoryProvider.provide(),
        beregningsgrunnlagRepository = repositoryProvider.provide(),
        beregningVurderingRepository = repositoryProvider.provide(),
        yrkesskadeRepository = repositoryProvider.provide(),
        manuellInntektGrunnlagRepository = repositoryProvider.provide(),
    )

    fun beregnGrunnlag(behandlingId: BehandlingId): Beregningsgrunnlag {
        val inntektGrunnlag = inntektGrunnlagRepository.hent(behandlingId)
        val manuellInntektGrunnlag = manuellInntektGrunnlagRepository.hentHvisEksisterer(behandlingId)
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
        val uføre = uføreRepository.hentHvisEksisterer(behandlingId)
        val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandlingId)

        val input = Inntektsbehov(
            // TODO: Hvor langt tilbake i tid skal man hente uføregrader?
            uføregrad = uføre?.vurderinger.orEmpty(),
            yrkesskadevurdering = sykdomGrunnlag?.yrkesskadevurdering,
            beregningGrunnlag = beregningVurdering,
            registrerteYrkesskader = yrkesskadeGrunnlag?.yrkesskader,
            inntektGrunnlag = inntektGrunnlag,
            manuelleInntekter = manuellInntektGrunnlag?.manuelleInntekter.orEmpty(),
        )

        val beregningsgrunnlag = beregneMedInput(input)

        beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag)
        return beregningsgrunnlag
    }

    fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        beregningsgrunnlagRepository.deaktiver(behandlingId)
    }

    fun utledRelevanteBeregningsÅr(behandlingId: BehandlingId): Set<Year> {
        val beregningGrunnlag = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        return Inntektsbehov.utledAlleRelevanteÅr(beregningGrunnlag)
    }

    companion object {

        fun beregneMedInput(input: Inntektsbehov): Beregningsgrunnlag {
            // 6G-begrensning ligger her samt gjennomsnitt
            val grunnlag11_19 = GrunnlagetForBeregningen(input.utledForOrdinær()).beregnGrunnlaget()

            val beregningMedEllerUtenUføre = if (input.finnesUføreData()) {
                input.validerSummertInntekt()
                UføreBeregning(grunnlag11_19, input).beregnUføre()
            } else {
                grunnlag11_19
            }

            // §11-22 Arbeidsavklaringspenger ved yrkesskade
            val beregningMedEllerUtenUføreMedEllerUtenYrkesskade =
                if (input.yrkesskadeVurderingEksisterer()) {
                    beregnGrunnlagYrkesskade(
                        grunnlag11_19 = beregningMedEllerUtenUføre,
                        antattÅrligInntekt = input.årligAntattInntektVedYrkesskade(),
                        andelAvNedsettelsenSomSkyldesYrkesskaden = input.andelYrkesskade()
                    )
                } else {
                    beregningMedEllerUtenUføre
                }
            return beregningMedEllerUtenUføreMedEllerUtenYrkesskade
        }
    }
}
