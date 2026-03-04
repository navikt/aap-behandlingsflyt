package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.RepositoryProvider
import java.math.BigDecimal
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

        val kombinertInntekt =
            kombinerInntektOgManuellInntekt(
                inntektGrunnlag.inntekter,
                manuellInntektGrunnlag?.manuelleInntekter.orEmpty()
            )

        val nedsettelsesdato = beregningVurdering?.tidspunktVurdering?.nedsattArbeidsevneEllerStudieevneDato
            ?: throw IllegalStateException("Nedsettelsesdato må være satt for beregning")
        val input = Inntektsbehov(
            nedsettelsesDato = nedsettelsesdato,
            årsInntekter = kombinertInntekt,
            // TODO: Hvor langt tilbake i tid skal man hente uføregrader?
            uføregrad = uføre?.vurderinger.orEmpty(),
            yrkesskadevurdering = sykdomGrunnlag?.yrkesskadevurdering,
            beregningGrunnlag = beregningVurdering,
            registrerteYrkesskader = yrkesskadeGrunnlag?.yrkesskader,
            inntektsPerioder = inntektGrunnlag.inntektPerMåned,
        )

        val beregningsgrunnlag = Beregning(input).beregneMedInput()

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

    fun kombinerInntektOgManuellInntekt(
        inntekter: Set<InntektPerÅr>,
        manuelleInntekter: Set<ManuellInntektVurdering>
    ): Set<InntektPerÅr> {
        val manuellePGIByÅr = manuelleInntekter
            .tilÅrInntekt { it.belop }

        val manuellEOSByÅr = manuelleInntekter
            .tilÅrInntekt { it.eøsBeløp }

        val inntekterByÅr = inntekter
            .groupBy { it.år }
            .mapValues {
                require(it.value.size == 1)
                it.value.first()
            }

        val kombinerteInntekter =
            (manuellePGIByÅr + inntekterByÅr).mapValues { (år, inntektPerÅr) ->
                val eos = manuellEOSByÅr[år]?.beløp ?: Beløp(BigDecimal.ZERO)
                inntektPerÅr.copy(beløp = inntektPerÅr.beløp.pluss(eos))
            }.values.toSet()

        return kombinerteInntekter
    }

    private fun Collection<ManuellInntektVurdering>.tilÅrInntekt(selector: (ManuellInntektVurdering) -> Beløp?): Map<Year, InntektPerÅr> {
        return this.filter { selector(it) != null }
            .map { InntektPerÅr(it.år, selector(it)!!, it) }
            .groupBy { it.år }
            .mapValues {
                it.value.single()
            }
    }
}
