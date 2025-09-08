package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Prosent

class UføreService(
    private val uføreRepository: UføreRepository,
    private val samordningUføreRepository: SamordningUføreRepository,
) {
    fun tidslinje(behandlingId: BehandlingId): Tidslinje<Prosent> {
        return samordningUføreRepository.hentHvisEksisterer(behandlingId)?.vurdering?.tilTidslinje()
            ?: Tidslinje.empty()
    }

    fun hentRegisterGrunnlagHvisEksisterer(behandlingId: BehandlingId): UføreGrunnlag? {
        return uføreRepository.hentHvisEksisterer(behandlingId)
    }

    fun hentVurderingGrunnlagHvisEksisterer(behandlingId: BehandlingId): SamordningUføreGrunnlag? {
        return samordningUføreRepository.hentHvisEksisterer(behandlingId)
    }
}