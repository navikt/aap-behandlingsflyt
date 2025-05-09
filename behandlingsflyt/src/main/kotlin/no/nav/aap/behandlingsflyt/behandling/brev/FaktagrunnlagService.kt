package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Repository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag.FristDato11_7
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Faktagrunnlag.GrunnlagBeregning
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class FaktagrunnlagService(
    private val behandlingRepository: BehandlingRepository,
    private val effektuer117repository: Effektuer11_7Repository,
    private val beregningService: BeregningService,
) {

    companion object {
        fun konstruer(repositoryProvider: RepositoryProvider): FaktagrunnlagService {
            return FaktagrunnlagService(
                behandlingRepository = repositoryProvider.provide<BehandlingRepository>(),
                effektuer117repository = repositoryProvider.provide<Effektuer11_7Repository>(),
                beregningService = BeregningService(repositoryProvider)
            )
        }
    }

    fun finnFaktagrunnlag(
        behandlingReferanse: BehandlingReferanse,
        faktagrunnlag: Set<FaktagrunnlagType>
    ): List<Faktagrunnlag> {
        val behandling = behandlingRepository.hent(behandlingReferanse)

        return faktagrunnlag.mapNotNull { hentFaktagrunnlag(behandling.id, it) }
    }

    private fun hentFaktagrunnlag(
        behandlingId: BehandlingId,
        faktagrunnlagType: FaktagrunnlagType,
    ): Faktagrunnlag? {
        return when (faktagrunnlagType) {
            FaktagrunnlagType.FRIST_DATO_11_7 -> hentFristDato11_7(behandlingId)

            FaktagrunnlagType.GRUNNLAG_BEREGNING -> hentGrunnlagBeregning(behandlingId)
        }
    }

    private fun hentFristDato11_7(behandlingId: BehandlingId): FristDato11_7? {
        val effektuer117grunnlag = effektuer117repository.hentHvisEksisterer(behandlingId)
        return effektuer117grunnlag?.varslinger?.lastOrNull()?.frist?.let { FristDato11_7(it) }
    }

    private fun hentGrunnlagBeregning(behandlingId: BehandlingId): GrunnlagBeregning? {
        val grunnlag: Beregningsgrunnlag = beregningService.beregnGrunnlag(behandlingId)
        return when (grunnlag) {
            is Grunnlag11_19 -> GrunnlagBeregning(
                grunnlag.inntekter().map { GrunnlagBeregning.InntektPerÅr(it.år, it.inntektIKroner.verdi()) }
            )

            is GrunnlagUføre -> null // TODO
            is GrunnlagYrkesskade -> null // TODO
        }
    }
}
