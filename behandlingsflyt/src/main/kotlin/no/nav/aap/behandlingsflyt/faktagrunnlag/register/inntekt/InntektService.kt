package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.behandling.beregning.AvklarFaktaBeregningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class InntektService private constructor(
    private val sakService: SakService,
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val avklarFaktaBeregningService: AvklarFaktaBeregningService,
    private val sykdomRepository: SykdomRepository,
    private val uføreRepository: UføreRepository,
    private val studentRepository: StudentRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val inntektRegisterGateway: InntektRegisterGateway
) : Informasjonskrav {

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val behandlingId = kontekst.behandlingId

        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        val inntekter = if (avklarFaktaBeregningService.skalFastsetteGrunnlag(behandlingId)) {
            val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
            val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId)
            val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandlingId)
            val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId)

            val sak = sakService.hent(kontekst.sakId)
            val nedsettelsesDato = utledNedsettelsesdato(beregningVurdering?.tidspunktVurdering, studentGrunnlag);
            val behov = Inntektsbehov(
                Input(
                    nedsettelsesDato = nedsettelsesDato,
                    inntekter = setOf(),
                    uføregrad = uføreGrunnlag?.vurdering?.uføregrad ?: Prosent(0),
                    yrkesskadevurdering = sykdomGrunnlag?.yrkesskadevurdering,
                    registrerteYrkesskader = yrkesskadeGrunnlag?.yrkesskader,
                    beregningGrunnlag = beregningVurdering
                )
            )
            val inntektsBehov = behov.utledAlleRelevanteÅr()

            inntektRegisterGateway.innhent(sak.person, inntektsBehov)
        } else {
            emptySet()
        }

        inntektGrunnlagRepository.lagre(behandlingId, inntekter)

        return if (eksisterendeGrunnlag?.inntekter == inntekter) IKKE_ENDRET else ENDRET
    }

    private fun utledNedsettelsesdato(
        beregningVurdering: BeregningstidspunktVurdering?,
        studentGrunnlag: StudentGrunnlag?
    ): LocalDate {
        val nedsettelsesdatoer = setOf(
            beregningVurdering?.nedsattArbeidsevneDato,
            studentGrunnlag?.studentvurdering?.avbruttStudieDato
        ).filterNotNull()

        return nedsettelsesdatoer.min()
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): InntektGrunnlag? {
        return inntektGrunnlagRepository.hentHvisEksisterer(behandlingId)
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): InntektService {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
            val beregningVurderingRepository = repositoryProvider.provide<BeregningVurderingRepository>()

            return InntektService(
                sakService = SakService(sakRepository),
                inntektGrunnlagRepository = repositoryProvider.provide(),
                avklarFaktaBeregningService = AvklarFaktaBeregningService(vilkårsresultatRepository),
                sykdomRepository = repositoryProvider.provide(),
                uføreRepository = repositoryProvider.provide(),
                studentRepository = repositoryProvider.provide(),
                beregningVurderingRepository = beregningVurderingRepository,
                yrkesskadeRepository = repositoryProvider.provide(),
                inntektRegisterGateway = InntektGateway
            )
        }
    }
}
