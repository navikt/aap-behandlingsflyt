package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektRegisterMock
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class InntektService private constructor(
    private val sakService: SakService,
    private val repository: InntektGrunnlagRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository
) : Grunnlag {

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): InntektService {
            return InntektService(
                SakService(connection),
                InntektGrunnlagRepository(connection),
                BeregningVurderingRepository(connection)
            )
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val behandlingId = kontekst.behandlingId
        val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(behandlingId)
        if (beregningVurdering?.nedsattArbeidsevneDato == null) {
            return false
        }
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        val nedsettelsesDato = beregningVurdering.nedsattArbeidsevneDato
        val behov = Inntektsbehov(Input(nedsettelsesDato = nedsettelsesDato))
        val inntektsBehov = behov.utledAlleRelevanteÅr()
        val sak = sakService.hent(kontekst.sakId)

        val register = InntektRegisterMock
        val inntekter = register.innhent(sak.person.identer(), inntektsBehov)

        repository.lagre(behandlingId, inntekter)

        val oppdatertGrunnlag = hentHvisEksisterer(behandlingId)

        return eksisterendeGrunnlag == oppdatertGrunnlag
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): InntektGrunnlag? {
        return repository.hentHvisEksisterer(behandlingId)
    }
}
