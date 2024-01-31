package no.nav.aap.behandlingsflyt.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.barn.BarnRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BarnetilleggService(
    private val barnRepository: BarnRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository
) {

    fun beregn(behandlingId: BehandlingId): Tidslinje<RettTilBarnetillegg> {

        val relevanteBarn = barnRepository.hentHvisEksisterer(behandlingId)?.barn ?: emptyList()
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)

        var resultat: Tidslinje<RettTilBarnetillegg> = Tidslinje(listOf())

        for (barn in relevanteBarn) {
            resultat = resultat.kombiner(
                Tidslinje(listOf(Segment(barn.periodeMedRettTil(), barn))),
                { periode, venstreSegment, høyreSegment ->
                    val venstreVerdi = venstreSegment?.verdi
                    val høyreVerdi = høyreSegment?.verdi
                    val nyVenstreVerdi = if (venstreVerdi == null) {
                        RettTilBarnetillegg()
                    } else {
                        venstreVerdi
                    }
                    if (høyreVerdi != null) {
                        nyVenstreVerdi.leggTilBarn(høyreVerdi.ident)
                    }

                    Segment(periode, nyVenstreVerdi)
                }, JoinStyle.CROSS_JOIN
            )
        }

        return resultat.disjoint(sak.rettighetsperiode)
    }
}