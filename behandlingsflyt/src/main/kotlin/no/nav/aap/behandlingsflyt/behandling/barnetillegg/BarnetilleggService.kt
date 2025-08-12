package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.IBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.outerJoin
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class BarnetilleggService(
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val barnRepository: BarnRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
        barnRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
    )

    fun beregn(behandlingId: BehandlingId): Tidslinje<RettTilBarnetillegg> {
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        var resultat: Tidslinje<RettTilBarnetillegg> =
            Tidslinje(listOf(Segment(sak.rettighetsperiode, RettTilBarnetillegg())))

        if (skalIkkeBeregneBarnetillegg(behandlingId)) {
            log.info("Behandling $behandlingId har ikke rett til barnetillegg, returnerer tom tidslinje.")
            return resultat
        }

        val barnGrunnlag = barnRepository.hent(behandlingId)
        val folkeregisterBarn =
            barnGrunnlag.registerbarn?.barn.orEmpty()
        val folkeregisterBarnTidslinje = tilTidslinje(folkeregisterBarn)

        resultat =
            resultat.kombiner(folkeregisterBarnTidslinje, JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                val venstreVerdi = venstreSegment.verdi.copy()
                if (høyreSegment?.verdi != null) {
                    venstreVerdi.leggTilFolkeregisterBarn(høyreSegment.verdi)
                }
                Segment(periode, venstreVerdi)
            })

        val vurderteBarn = barnGrunnlag.vurderteBarn?.barn.orEmpty()
        val vurderteBarnIdenter = vurderteBarn.map { it.ident }
        val oppgittBarnSomIkkeErVurdert =
            barnGrunnlag.oppgitteBarn?.oppgitteBarn
                ?.filterNot { vurderteBarnIdenter.contains(it.identifikator()) }
                .orEmpty()

        val oppgittBarnTidslinje = tilTidslinje(oppgittBarnSomIkkeErVurdert)
        resultat =
            resultat.kombiner(oppgittBarnTidslinje, JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                val venstreVerdi = venstreSegment.verdi.copy()
                if (høyreSegment?.verdi != null) {
                    venstreVerdi.leggTilOppgitteBarn(høyreSegment.verdi)
                }
                Segment(periode, venstreVerdi)
            })

        for (barn in vurderteBarn) {
            resultat = resultat.kombiner(
                barn.tilTidslinje(),
                // Outer join siden vurderte barn kan ha prioritet
                JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
                    val høyreVerdi = høyreSegment?.verdi
                    val nyVenstreVerdi = venstreSegment?.verdi?.copy() ?: RettTilBarnetillegg()
                    if (høyreVerdi != null) {
                        if (høyreVerdi.harForeldreAnsvar) {
                            nyVenstreVerdi.godkjenteBarn(setOf(barn.ident))
                        } else {
                            nyVenstreVerdi.underkjenteBarn(setOf(barn.ident))
                        }
                    }

                    Segment(periode, nyVenstreVerdi)
                })
        }

        return resultat.begrensetTil(sak.rettighetsperiode)
    }

    private fun skalIkkeBeregneBarnetillegg(behandlingId: BehandlingId): Boolean {
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        return !(sykdomsvilkåret.harPerioderSomErOppfylt() && bistandsvilkåret.harPerioderSomErOppfylt())
    }

    private fun tilTidslinje(barna: List<IBarn>): Tidslinje<Set<BarnIdentifikator>> {
        return barna
            .map { barnet ->
                Tidslinje(
                    listOf(
                        Segment(
                            Barn.periodeMedRettTil(barnet.fødselsdato()),
                            barnet.identifikator()
                        )
                    )
                )
            }
            .outerJoin { t -> t.toSet() }
    }
}
