package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class InntektService private constructor(
    private val sakService: SakService,
    private val inntektGrunnlagRepository: InntektGrunnlagRepository,
    private val sykdomRepository: SykdomRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val inntektRegisterGateway: InntektRegisterGateway
) : Informasjonskrav {

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val behandlingId = kontekst.behandlingId
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId)
        if (sykdomGrunnlag?.sykdomsvurdering?.nedsattArbeidsevneDato == null) {
            return false
        }
        val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)

        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        val nedsettelsesDato = sykdomGrunnlag.sykdomsvurdering.nedsattArbeidsevneDato.atMonth(1).atDay(1)
        val behov = Inntektsbehov(Input(
            nedsettelsesDato = nedsettelsesDato,
            inntekter = setOf(),
            uføregrad = Prosent.`0_PROSENT`,
            yrkesskadevurdering = sykdomGrunnlag.yrkesskadevurdering,
            beregningVurdering = beregningVurdering
        ))
        val inntektsBehov = behov.utledAlleRelevanteÅr()

        val sak = sakService.hent(kontekst.sakId)

        val inntekter = inntektRegisterGateway.innhent(sak.person, inntektsBehov)

        inntektGrunnlagRepository.lagre(behandlingId, inntekter)

        return eksisterendeGrunnlag?.inntekter == inntekter
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): InntektGrunnlag? {
        return inntektGrunnlagRepository.hentHvisEksisterer(behandlingId)
    }

    companion object : Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): InntektService {
            return InntektService(
                SakService(connection),
                InntektGrunnlagRepository(connection),
                SykdomRepository(connection),
                BeregningVurderingRepository(connection),
                InntektGateway
            )
        }
    }
}
