package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Year

class InntektInformasjonskrav(
    private val sakService: SakService,
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val studentRepository: StudentRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val inntektRegisterGateway: InntektRegisterGateway,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav {

    private val log = LoggerFactory.getLogger(javaClass)

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                (oppdatert.ikkeKjørtSisteKalenderdag() || relevanteÅrErEndret(kontekst)) &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    private fun relevanteÅrErEndret(kontekst: FlytKontekstMedPerioder): Boolean {
        val relevanteÅrEksisterendeGrunnlag = hentHvisEksisterer(kontekst.behandlingId)?.inntekter?.map { it.år }?.toSet() ?: emptySet()
        val relevanteÅrFraGjeldendeInntektsbehov = utledAlleRelevanteÅr(kontekst.behandlingId)

        return relevanteÅrEksisterendeGrunnlag != relevanteÅrFraGjeldendeInntektsbehov
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val behandlingId = kontekst.behandlingId
        val eksisterendeGrunnlag = inntektGrunnlagRepository.hentHvisEksisterer(behandlingId)

        val relevanteÅr = utledAlleRelevanteÅr(behandlingId)
        val sak = sakService.hent(kontekst.sakId)

        log.info("Henter inntekter for følgende år: $relevanteÅr")
        val oppdaterteInntekter = inntektRegisterGateway.innhent(sak.person, relevanteÅr)

        inntektGrunnlagRepository.lagre(behandlingId, oppdaterteInntekter)

        return if (eksisterendeGrunnlag?.inntekter == oppdaterteInntekter) IKKE_ENDRET else ENDRET
    }

    private fun utledAlleRelevanteÅr(behandlingId: BehandlingId): Set<Year> {
        val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId)
        val beregningGrunnlag = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        return Inntektsbehov.utledAlleRelevanteÅr(beregningGrunnlag, studentGrunnlag)
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): InntektGrunnlag? {
        return inntektGrunnlagRepository.hentHvisEksisterer(behandlingId)
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.INNTEKT

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): InntektInformasjonskrav {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val beregningVurderingRepository = repositoryProvider.provide<BeregningVurderingRepository>()

            return InntektInformasjonskrav(
                sakService = SakService(sakRepository),
                inntektGrunnlagRepository = repositoryProvider.provide(),
                studentRepository = repositoryProvider.provide(),
                beregningVurderingRepository = beregningVurderingRepository,
                inntektRegisterGateway = gatewayProvider.provide(),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }
}
