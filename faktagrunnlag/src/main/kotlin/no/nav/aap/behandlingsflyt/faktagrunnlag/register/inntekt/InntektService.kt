package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.Vurdering
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate

class InntektService private constructor(
    private val sakService: SakService,
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykdomRepository: SykdomRepository,
    private val studentRepository: StudentRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val inntektRegisterGateway: InntektRegisterGateway
) : Informasjonskrav {

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val behandlingId = kontekst.behandlingId
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)

        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        val inntekter = if (skalInnhenteOpplysninger(vilkårsresultat)) {
            val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
            val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId)
            val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandlingId)

            val sak = sakService.hent(kontekst.sakId)
            val nedsettelsesDato = utledNedsettelsesdato(beregningVurdering?.tidspunktVurdering, studentGrunnlag);
            val behov = Inntektsbehov(
                Input(
                    nedsettelsesDato = nedsettelsesDato,
                    inntekter = setOf(),
                    uføregrad = Prosent.`0_PROSENT`,
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

    private fun skalInnhenteOpplysninger(vilkårsresultat: Vilkårsresultat): Boolean {
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        return sykdomsvilkåret.harPerioderSomErOppfylt() && bistandsvilkåret.harPerioderSomErOppfylt()
    }

    private fun utledNedsettelsesdato(beregningVurdering: BeregningstidspunktVurdering?, studentGrunnlag: StudentGrunnlag?): LocalDate {
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
        override fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
            // Skal kun innhente på nytt når det skal beregnes, førstegengasbehandling
            return kontekst.behandlingType == TypeBehandling.Førstegangsbehandling || skalReberegne(kontekst.perioderTilVurdering)
        }

        private fun skalReberegne(vurderinger: Set<Vurdering>): Boolean {
            return false
        }

        override fun konstruer(connection: DBConnection): InntektService {
            return InntektService(
                SakService(SakRepositoryImpl(connection)),
                InntektGrunnlagRepository(connection),
                VilkårsresultatRepository(connection),
                SykdomRepository(connection),
                StudentRepository(connection),
                BeregningVurderingRepository(connection),
                YrkesskadeRepository(connection),
                InntektGateway
            )
        }
    }
}
