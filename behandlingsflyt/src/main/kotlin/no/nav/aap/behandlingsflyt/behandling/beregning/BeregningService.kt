package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.behandling.beregning.Beregning.Companion.kombinerInntektOgManuellInntekt
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
        val uføregrad = uføreRepository.hentHvisEksisterer(behandlingId)?.vurderinger.orEmpty()
        val yrkesskadevurdering = sykdomRepository.hentHvisEksisterer(behandlingId)?.yrkesskadevurdering
        val beregningGrunnlag = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        val registrerteYrkesskader = yrkesskadeRepository.hentHvisEksisterer(behandlingId)?.yrkesskader
        val inntektGrunnlag = inntektGrunnlagRepository.hent(behandlingId)
        val manuelleInntekter = manuellInntektGrunnlagRepository.hentHvisEksisterer(behandlingId)?.manuelleInntekter.orEmpty()

        val input = Beregning(
            årsInntekter = kombinerInntektOgManuellInntekt(inntektGrunnlag.inntekter, manuelleInntekter),
            nedsettelsesDato = beregningGrunnlag?.tidspunktVurdering?.nedsattArbeidsevneEllerStudieevneDato
                ?: throw IllegalStateException("Nedsettelsesdato må være satt for beregning"),
            ytterligereNedsettelsesDato = beregningGrunnlag.tidspunktVurdering.ytterligereNedsattArbeidsevneDato,
            inntektsPerioder = inntektGrunnlag.inntektPerMåned,
            // TODO: Hvor langt tilbake i tid skal man hente uføregrader?
            uføregrad = uføregrad,
            yrkesskadevurdering = yrkesskadevurdering,
            registrerteYrkesskader = registrerteYrkesskader,
            yrkesskadeBeløpVurderinger = beregningGrunnlag.yrkesskadeBeløpVurdering?.vurderinger,
        )

        val beregningsgrunnlag = input.beregnBeregningsgrunnlag()

        beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag)
        return beregningsgrunnlag
    }

    fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        beregningsgrunnlagRepository.deaktiver(behandlingId)
    }

    fun utledRelevanteBeregningsÅr(behandlingId: BehandlingId): Set<Year> {
        val beregningGrunnlag = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        return Beregning.utledAlleRelevanteÅr(beregningGrunnlag)
    }
}
