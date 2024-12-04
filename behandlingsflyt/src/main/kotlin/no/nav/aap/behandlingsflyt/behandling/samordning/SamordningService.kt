package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje

class SamordningService(
    private val samordningYtelseVurderingRepository: SamordningYtelseVurderingRepository,
    private val underveisRepository: UnderveisRepository,
) {
    fun vurder(behandlingId: BehandlingId): Tidslinje<SamordningGradering> {
        val samordningYtelseVurderingGrunnlag = samordningYtelseVurderingRepository.hentHvisEksisterer(behandlingId)

        //TODO: Kan benytte denne til å filtrere perioder hvor det ikke er rett på ytelse uansett
        underveisRepository.hentHvisEksisterer(behandlingId)

        val vurderRegler = vurderRegler(samordningYtelseVurderingGrunnlag)

        return vurderRegler
    }

    fun harGjortVurdering(behandlingId: BehandlingId): Boolean {
        val samordningYtelseVurderingGrunnlag = samordningYtelseVurderingRepository.hentHvisEksisterer(behandlingId)

        return samordningYtelseVurderingGrunnlag?.vurderingerId != null
            && samordningYtelseVurderingGrunnlag.vurderinger != null
            && samordningYtelseVurderingGrunnlag.vurderinger!!.isNotEmpty()
    }

    private fun vurderRegler(samordning: SamordningYtelseVurderingGrunnlag?) : Tidslinje<SamordningGradering> {
        //TODO: Kombiner til tidslinje her. Kaja skal utarbeide en oversikt over regler, avventer dette
        return Tidslinje(listOf())
    }
}