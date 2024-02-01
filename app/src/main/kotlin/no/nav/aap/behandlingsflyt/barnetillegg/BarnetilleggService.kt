package no.nav.aap.behandlingsflyt.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.barn.BarnRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
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

        var resultat: Tidslinje<RettTilBarnetillegg> =
            Tidslinje(listOf(Segment(sak.rettighetsperiode, RettTilBarnetillegg())))

        for (barn in relevanteBarn) {
            resultat = resultat.kombiner(
                Tidslinje(listOf(Segment(barn.periodeMedRettTil(), barn)))
            ) { periode, venstreSegment, høyreSegment ->
                val høyreVerdi = høyreSegment?.verdi
                val nyVenstreVerdi = venstreSegment?.verdi ?: RettTilBarnetillegg()
                if (høyreVerdi != null) {
                    nyVenstreVerdi.leggTilBarn(høyreVerdi.ident)
                }

                Segment(periode, nyVenstreVerdi)
            }
        }

        return resultat.kryss(sak.rettighetsperiode)
    }
}