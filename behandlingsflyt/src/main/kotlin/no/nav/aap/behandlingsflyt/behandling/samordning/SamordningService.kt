package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje

class SamordningService(
    private val samordningYtelseVurderingRepository: SamordningYtelseVurderingRepository,
    private val underveisRepository: UnderveisRepository,
) {
    fun vurder(behandlingId: BehandlingId): Tidslinje<SamordningGradering> {
        val samordningYtelseVurderingGrunnlag = samordningYtelseVurderingRepository.hentHvisEksisterer(behandlingId)

        //TODO: Kan benytte denne til å filtrere perioder hvor det ikke er rett på ytelse uansett
        val underveisPerioder = underveisRepository.hentHvisEksisterer(behandlingId)

        if (samordningYtelseVurderingGrunnlag == null) {
            return Tidslinje(emptyList())
        }
        val vurderRegler = vurderRegler(samordningYtelseVurderingGrunnlag)

        return vurderRegler
    }

    fun harGjortVurdering(behandlingId: BehandlingId): Boolean {
        val samordningYtelseVurderingGrunnlag = samordningYtelseVurderingRepository.hentHvisEksisterer(behandlingId)

        return samordningYtelseVurderingGrunnlag?.vurderingerId != null && !samordningYtelseVurderingGrunnlag.vurderinger.isNullOrEmpty()
    }

    private fun vurderRegler(samordning: SamordningYtelseVurderingGrunnlag): Tidslinje<SamordningGradering> {
        // Desperat forsøk på å lage en tidslinje
        // TODO: verifiser logikk osv
        val p = samordning.ytelser.flatMap {
            it.ytelsePerioder.map { ytelsePeriode -> Pair(it, ytelsePeriode) }
        }.map { (ytelse, ytelsePeriode) ->
            Segment(
                ytelsePeriode.periode, SamordningGradering(
                    gradering = ytelsePeriode.gradering!!, ytelsesGraderinger = ytelse.ytelsePerioder.map {
                        YtelseGradering(
                            ytelse = ytelse.ytelseType, gradering = it.gradering!!
                        )
                    })
            )
        }
        return Tidslinje(p)

    }
}