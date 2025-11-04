package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Year

class BeregningService(
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val sykdomRepository: SykdomRepository,
    private val studentRepository: StudentRepository,
    private val uføreRepository: UføreRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository,
    // TODO: Kan det skje at denne ikke har data langt nok tilbake i tid?
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository
) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        inntektGrunnlagRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        studentRepository = repositoryProvider.provide(),
        uføreRepository = repositoryProvider.provide(),
        beregningsgrunnlagRepository = repositoryProvider.provide(),
        beregningVurderingRepository = repositoryProvider.provide(),
        yrkesskadeRepository = repositoryProvider.provide(),
        manuellInntektGrunnlagRepository = repositoryProvider.provide(),
        medlemskapArbeidInntektRepository = repositoryProvider.provide(),
    )

    fun beregnGrunnlag(behandlingId: BehandlingId): Beregningsgrunnlag {
        val inntektGrunnlag = inntektGrunnlagRepository.hent(behandlingId)
        val manuellInntektGrunnlag = manuellInntektGrunnlagRepository.hentHvisEksisterer(behandlingId)
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
        val uføre = uføreRepository.hentHvisEksisterer(behandlingId)
        val student = studentRepository.hentHvisEksisterer(behandlingId)
        val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandlingId)
        val inntektsPerioder = requireNotNull(medlemskapArbeidInntektRepository.hentHvisEksisterer(behandlingId)?.inntekterINorgeGrunnlag) {
            "Må ha hentet inntekt i lovvalgssteget før grunnlagsberegningen"
        }.map { InntektsPeriode(
            periode = it.periode,
            beløp = it.beloep,
            inntektType = it.inntektType
        ) }


        val kombinertInntekt =
            kombinerInntektOgManuellInntekt(
                inntektGrunnlag.inntekter,
                manuellInntektGrunnlag?.manuelleInntekter.orEmpty()
            )

        val input = utledInput(
            studentVurdering = student?.studentvurdering,
            yrkesskadevurdering = sykdomGrunnlag?.yrkesskadevurdering,
            vurdering = beregningVurdering,
            årsInntekter = kombinertInntekt,
            // TODO: Hvor langt tilbake i tid skal man hente uføregrader?
            uføregrad = uføre?.vurderinger.orEmpty(),
            registrerteYrkesskader = yrkesskadeGrunnlag?.yrkesskader,
            inntektsPerioder = inntektsPerioder
        )

        val beregning = Beregning(input)
        val beregningsgrunnlag = beregning.beregneMedInput()

        beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag)
        return beregningsgrunnlag
    }

    fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        beregningsgrunnlagRepository.deaktiver(behandlingId)
    }

    fun utledRelevanteBeregningsÅr(behandlingId: BehandlingId): Set<Year> {
        val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId)
        val beregningGrunnlag = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        return Inntektsbehov.utledAlleRelevanteÅr(beregningGrunnlag, studentGrunnlag)
    }

    private fun kombinerInntektOgManuellInntekt(
        inntekter: Set<InntektPerÅr>,
        manuelleInntekter: Set<ManuellInntektVurdering>
    ): Set<InntektPerÅr> {
        val manuelleByÅr = manuelleInntekter
            .map { InntektPerÅr(it.år, it.belop, it) }
            .groupBy { it.år }
            .mapValues {
                require(it.value.size == 1)
                it.value.first()
            }

        val inntekterByÅr = inntekter
            .groupBy { it.år }
            .mapValues {
                require(it.value.size == 1)
                it.value.first()
            }

        // Hvis begge deler finnes for samme år, foretrekkes verdien fra register
        val kombinerteInntekter = (manuelleByÅr + inntekterByÅr).values.toSet()
        return kombinerteInntekter
    }

    private fun utledInput(
        studentVurdering: StudentVurdering?,
        yrkesskadevurdering: Yrkesskadevurdering?,
        vurdering: BeregningGrunnlag?,
        årsInntekter: Set<InntektPerÅr>,
        inntektsPerioder: List<InntektsPeriode>,
        uføregrad: List<Uføre>,
        registrerteYrkesskader: Yrkesskader?
    ): Inntektsbehov {
        return Inntektsbehov(
            Input(
                nedsettelsesDato = Inntektsbehov.utledNedsettelsesdato(vurdering?.tidspunktVurdering, studentVurdering),
                årsInntekter = årsInntekter,
                uføregrad = uføregrad,
                yrkesskadevurdering = yrkesskadevurdering,
                beregningGrunnlag = vurdering,
                registrerteYrkesskader = registrerteYrkesskader,
                inntektsPerioder = inntektsPerioder,
            )
        )
    }
}
