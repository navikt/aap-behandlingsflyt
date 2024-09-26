package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository

import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class SamordningService(
    private val samordningYtelseVurderingRepository: SamordningYtelseVurderingRepository
) {
    fun vurder(behandlingId: BehandlingId): Tidslinje<SamordningGradering> {
        val samordningYtelseVurderingGrunnlag = samordningYtelseVurderingRepository.hentHvisEksisterer(behandlingId)
        val vurderRegler = vurderRegler(samordningYtelseVurderingGrunnlag)

        return vurderRegler
    }

    fun harGjortVurdering(behandlingId: BehandlingId): Boolean {
        val samordningYtelseVurderingGrunnlag = samordningYtelseVurderingRepository.hentHvisEksisterer(behandlingId)

        return samordningYtelseVurderingGrunnlag?.vurderingerId != null
            && samordningYtelseVurderingGrunnlag.vurderinger != null
            && samordningYtelseVurderingGrunnlag.vurderinger!!.isNotEmpty()
    }

    fun vurderRegler(samordning: SamordningYtelseVurderingGrunnlag?) : Tidslinje<SamordningGradering> {
        //TODO: Kombiner til tidslinje her. Kaja skal utarbeide en oversikt over regler, avventer detteø
        return Tidslinje(listOf())
    }
}