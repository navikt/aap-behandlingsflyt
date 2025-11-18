package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Year

class InntektInformasjonskrav(
    private val sakService: SakService,
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val studentRepository: StudentRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val inntektRegisterGateway: InntektRegisterGateway,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav<InntektInformasjonskrav.InntektInput, InntektInformasjonskrav.InntektRegisterdata> {

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg) &&
                // Avhengig av å vite hvilke år vi skal hente inntekt for
                kanUtledeRelevanteÅr(kontekst) &&
                // Oppdaterer inntekt uavhengig av om relevante år har endret seg en gang i døgnet / hvis ikke hver gang relevante år endrer seg
                (oppdatert.ikkeKjørtSisteKalenderdag() || relevanteÅrErEndret(kontekst))
    }

    data class InntektRegisterdata(val inntekter: Set<InntektPerÅr>) : InformasjonskravRegisterdata

    data class InntektInput(val person: Person, val relevanteÅr: Set<Year>) : InformasjonskravInput

    override fun klargjør(kontekst: FlytKontekstMedPerioder): InntektInput {
        val sak = sakService.hent(kontekst.sakId)
        val relevanteÅr = utledAlleRelevanteÅr(kontekst.behandlingId)
        return InntektInput(sak.person, relevanteÅr)
    }

    override fun hentData(input: InntektInput): InntektRegisterdata {
        val (person, relevanteÅr) = input
        val oppdaterteInntekter = inntektRegisterGateway.innhent(person, relevanteÅr)

        return InntektRegisterdata(oppdaterteInntekter)
    }

    override fun oppdater(
        input: InntektInput,
        registerdata: InntektRegisterdata,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        val behandlingId = kontekst.behandlingId

        val inntektGrunnlag = inntektGrunnlagRepository.hentHvisEksisterer(behandlingId)
        val oppdaterteInntekter = registerdata.inntekter

        if (inntektGrunnlag?.inntekter == oppdaterteInntekter) {
            return IKKE_ENDRET
        } else {
            inntektGrunnlagRepository.lagre(behandlingId, oppdaterteInntekter)
            return ENDRET
        }
    }

    private fun kanUtledeRelevanteÅr(kontekst: FlytKontekstMedPerioder): Boolean {
        val studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
        val beregningGrunnlag = beregningVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)

        val nedsattArbeidsevneDato = beregningGrunnlag?.tidspunktVurdering?.nedsattArbeidsevneDato
        val avbruttStudieDato = studentGrunnlag?.studentvurdering?.avbruttStudieDato
        return nedsattArbeidsevneDato != null || avbruttStudieDato != null
    }

    private fun relevanteÅrErEndret(kontekst: FlytKontekstMedPerioder): Boolean {
        val relevanteÅrEksisterendeGrunnlag =
            hentHvisEksisterer(kontekst.behandlingId)?.inntekter?.map { it.år }?.toSet() ?: emptySet()
        val relevanteÅrFraGjeldendeInntektsbehov = utledAlleRelevanteÅr(kontekst.behandlingId)

        return relevanteÅrEksisterendeGrunnlag != relevanteÅrFraGjeldendeInntektsbehov
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

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): InntektInformasjonskrav {
            val beregningVurderingRepository = repositoryProvider.provide<BeregningVurderingRepository>()

            return InntektInformasjonskrav(
                sakService = SakService(repositoryProvider),
                inntektGrunnlagRepository = repositoryProvider.provide(),
                studentRepository = repositoryProvider.provide(),
                beregningVurderingRepository = beregningVurderingRepository,
                inntektRegisterGateway = gatewayProvider.provide(),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }
}
