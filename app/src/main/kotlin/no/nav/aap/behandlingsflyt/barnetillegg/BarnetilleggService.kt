package no.nav.aap.behandlingsflyt.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakOgBehandlingService
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

//TODO: Må se på om faktagrunnlag.barn skal deles i to, en for registeropplysninger og en for delvurdering fra saksbehandler
class BarnetilleggService(
    private val barnRepository: BarnRepository,
    private val barnetilleggRepository: BarnetilleggRepository,
    private val sakOgBehandlingService: SakOgBehandlingService
) {
    fun beregn(behandlingId: BehandlingId): Tidslinje<RettTilBarnetillegg> {
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        var resultat: Tidslinje<RettTilBarnetillegg> =
            Tidslinje(listOf(Segment(sak.rettighetsperiode, RettTilBarnetillegg())))

        val relevanteBarn = barnRepository.hentHvisEksisterer(behandlingId)?.barn ?: emptyList()

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

        barnetilleggRepository.lagre(
            behandlingId,
            resultat.segmenter()
                .map {
                    BarnetilleggPeriode(
                        it.periode,
                        it.verdi.barn()
                    )
                }
        )

        return resultat.kryss(sak.rettighetsperiode)
    }
}