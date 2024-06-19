package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate

class InntektService private constructor(
    private val sakService: SakService,
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykdomRepository: SykdomRepository,
    private val studentRepository: StudentRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val inntektRegisterGateway: InntektRegisterGateway
) : Informasjonskrav {

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val behandlingId = kontekst.behandlingId
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        if (skalIkkeInnhenteOpplysninger(vilkårsresultat)) {
            return false
        }
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
        val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId)
        val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)

        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        val nedsettelsesDato = utledNedsettelsesdato(sykdomGrunnlag, studentGrunnlag);
        val behov = Inntektsbehov(
            Input(
                nedsettelsesDato = nedsettelsesDato,
                inntekter = setOf(),
                uføregrad = Prosent.`0_PROSENT`,
                yrkesskadevurdering = sykdomGrunnlag?.yrkesskadevurdering,
                beregningVurdering = beregningVurdering
            )
        )
        val inntektsBehov = behov.utledAlleRelevanteÅr()

        val sak = sakService.hent(kontekst.sakId)

        val inntekter = inntektRegisterGateway.innhent(sak.person, inntektsBehov)

        inntektGrunnlagRepository.lagre(behandlingId, inntekter)

        return eksisterendeGrunnlag?.inntekter == inntekter
    }

    private fun skalIkkeInnhenteOpplysninger(vilkårsresultat: Vilkårsresultat): Boolean {
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        return !(sykdomsvilkåret.harPerioderSomErOppfylt() && bistandsvilkåret.harPerioderSomErOppfylt())
    }

    private fun utledNedsettelsesdato(sykdomGrunnlag: SykdomGrunnlag?, studentGrunnlag: StudentGrunnlag?): LocalDate {
        val nedsettelsesdatoer = setOf(
            sykdomGrunnlag?.sykdomsvurdering?.nedsattArbeidsevneDato?.let { LocalDate.of(it, 1, 1) },
            studentGrunnlag?.studentvurdering?.avbruttStudieDato
        ).filterNotNull()

        return nedsettelsesdatoer.min()
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): InntektGrunnlag? {
        return inntektGrunnlagRepository.hentHvisEksisterer(behandlingId)
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): InntektService {
            return InntektService(
                SakService(connection),
                InntektGrunnlagRepository(connection),
                VilkårsresultatRepository(connection),
                SykdomRepository(connection),
                StudentRepository(connection),
                BeregningVurderingRepository(connection),
                InntektGateway
            )
        }
    }
}
