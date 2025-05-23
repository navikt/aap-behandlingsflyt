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
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSiste
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FastsettGrunnlagSteg
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate

class InntektService private constructor(
    private val sakService: SakService,
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val sykdomRepository: SykdomRepository,
    private val uføreRepository: UføreRepository,
    private val studentRepository: StudentRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
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
                oppdatert.ikkeKjørtSiste(Duration.ofHours(1)) &&
                tidligereVurderinger.harBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val behandlingId = kontekst.behandlingId

        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        val inntekter =
            if (!tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, FastsettGrunnlagSteg.type())) {
                val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
                val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId)
                val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
                val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandlingId)
                val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId)

                val sak = sakService.hent(kontekst.sakId)
                if (beregningVurdering?.tidspunktVurdering?.nedsattArbeidsevneDato == null && studentGrunnlag?.studentvurdering?.avbruttStudieDato == null) {
                    log.error("Verken tidspunktVurdering eller studentGrunnlag fantes. Returner IKKE_ENDRET.")
                    return IKKE_ENDRET
                }
                val nedsettelsesDato = utledNedsettelsesdato(
                    beregningVurdering?.tidspunktVurdering?.nedsattArbeidsevneDato,
                    studentGrunnlag?.studentvurdering?.avbruttStudieDato
                )
                val behov = Inntektsbehov(
                    Input(
                        nedsettelsesDato = nedsettelsesDato,
                        inntekter = setOf(),
                        uføregrad = uføreGrunnlag?.vurderinger ?: emptyList(),
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
        nedsattArbeidsevneDato: LocalDate?,
        avbruttStudieDato: LocalDate?
    ): LocalDate {
        val nedsettelsesdatoer = setOf(
            nedsattArbeidsevneDato,
            avbruttStudieDato
        ).filterNotNull()

        return nedsettelsesdatoer.min()
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): InntektGrunnlag? {
        return inntektGrunnlagRepository.hentHvisEksisterer(behandlingId)
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.INNTEKT

        override fun konstruer(repositoryProvider: RepositoryProvider): InntektService {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val beregningVurderingRepository = repositoryProvider.provide<BeregningVurderingRepository>()

            return InntektService(
                sakService = SakService(sakRepository),
                inntektGrunnlagRepository = repositoryProvider.provide(),
                sykdomRepository = repositoryProvider.provide(),
                uføreRepository = repositoryProvider.provide(),
                studentRepository = repositoryProvider.provide(),
                beregningVurderingRepository = beregningVurderingRepository,
                yrkesskadeRepository = repositoryProvider.provide(),
                inntektRegisterGateway = GatewayProvider.provide(),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }
}
