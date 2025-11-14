package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.IBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn.OppgittBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.SaksbehandlerOppgitteBarn.SaksbehandlerOppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.outerJoin
import no.nav.aap.lookup.repository.RepositoryProvider

class BarnetilleggService(
    private val barnRepository: BarnRepository,
    private val sakService: SakService,
) {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        barnRepository = repositoryProvider.provide(),
        sakService = SakService(repositoryProvider.provide(), repositoryProvider.provide())
    )

    fun beregn(behandlingId: BehandlingId): Tidslinje<RettTilBarnetillegg> {
        val sak = sakService.hentSakFor(behandlingId)
        val barnGrunnlag = barnRepository.hent(behandlingId)

        var resultat: Tidslinje<RettTilBarnetillegg> =
            Tidslinje(listOf(Segment(sak.rettighetsperiode, RettTilBarnetillegg())))

        resultat = kombinerFolkeregisterBarn(resultat, barnGrunnlag)
        resultat = kombinerOppgitteBarn(resultat, barnGrunnlag)
        resultat = kombinerSaksbehandlerOppgitteBarn(resultat, barnGrunnlag)
        resultat = kombinerVurderteBarn(resultat, barnGrunnlag)

        return resultat.begrensetTil(sak.rettighetsperiode)
    }

    private fun kombinerFolkeregisterBarn(
        tidslinje: Tidslinje<RettTilBarnetillegg>,
        barnGrunnlag: BarnGrunnlag
    ): Tidslinje<RettTilBarnetillegg> {
        val folkeregisterBarn = barnGrunnlag.registerbarn?.barn.orEmpty()
        val vurderteBarn = barnGrunnlag.vurderteBarn?.barn.orEmpty()

        val barnUtenVurdering = folkeregisterBarn.filter { barn ->
            vurderteBarn.none { it.ident.er(barn.ident) && it.vurderinger.isNotEmpty() }
        }

        return kombinerMedLeftJoin(tidslinje, tilTidslinje(barnUtenVurdering)) { barn ->
            leggTilFolkeregisterBarn(barn)
        }
    }

    private fun kombinerOppgitteBarn(
        tidslinje: Tidslinje<RettTilBarnetillegg>,
        barnGrunnlag: BarnGrunnlag
    ): Tidslinje<RettTilBarnetillegg> {
        val folkeregisterBarn = barnGrunnlag.registerbarn?.barn.orEmpty()
        val vurderteBarnIdenter = barnGrunnlag.vurderteBarn?.barn?.map { it.ident }.orEmpty()
        val oppgittBarnSomIkkeErVurdert =
            barnGrunnlag.oppgitteBarn?.oppgitteBarn
                ?.filterNot { oppgittBarn ->
                    vurderteBarnIdenter.contains(oppgittBarn.identifikator()) ||
                            folkeregisterBarn.any { it.ident.er(oppgittBarn.identifikator()) }
                }
                .orEmpty()

        return kombinerMedLeftJoin(tidslinje, tilTidslinje(oppgittBarnSomIkkeErVurdert)) { barn ->
            leggTilUavklartBarn(barn)
        }
    }

    private fun kombinerSaksbehandlerOppgitteBarn(
        tidslinje: Tidslinje<RettTilBarnetillegg>,
        barnGrunnlag: BarnGrunnlag
    ): Tidslinje<RettTilBarnetillegg> {
        val vurderteBarnIdenter = barnGrunnlag.vurderteBarn?.barn?.map { it.ident }.orEmpty()
        val saksbehandlerOppgittBarnSomIkkeErVurdert = barnGrunnlag.saksbehandlerOppgitteBarn?.barn
            ?.filterNot { vurderteBarnIdenter.contains(it.identifikator()) }
            .orEmpty()

        return kombinerMedLeftJoin(tidslinje, tilTidslinje(saksbehandlerOppgittBarnSomIkkeErVurdert)) { barn ->
            leggTilUavklartBarn(barn)
        }
    }

    private fun kombinerVurderteBarn(
        tidslinje: Tidslinje<RettTilBarnetillegg>,
        barnGrunnlag: BarnGrunnlag
    ): Tidslinje<RettTilBarnetillegg> {
        val vurderteBarn = barnGrunnlag.vurderteBarn?.barn.orEmpty()
        var resultat = tidslinje

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

        return resultat
    }

    private fun kombinerMedLeftJoin(
        tidslinje: Tidslinje<RettTilBarnetillegg>,
        barnTidslinje: Tidslinje<Set<BarnIdentifikator>>,
        leggTilBarn: RettTilBarnetillegg.(Set<BarnIdentifikator>) -> Unit
    ): Tidslinje<RettTilBarnetillegg> {
        return tidslinje.kombiner(
            barnTidslinje,
            JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                val venstreVerdi = venstreSegment.verdi.copy()
                if (høyreSegment?.verdi != null) {
                    venstreVerdi.leggTilBarn(høyreSegment.verdi)
                }
                Segment(periode, venstreVerdi)
            })
    }

    private fun tilTidslinje(barna: List<IBarn>): Tidslinje<Set<BarnIdentifikator>> {
        return barna
            .map { barnet ->
                Tidslinje(
                    listOf(
                        Segment(
                            Barn.periodeMedRettTil(
                                barnet.fødselsdato(), when (barnet) {
                                    is Barn -> barnet.dødsdato
                                    is OppgittBarn -> null
                                    is SaksbehandlerOppgitteBarn -> null
                                }
                            ),
                            barnet.identifikator()
                        )
                    )
                )
            }
            .outerJoin { t -> t.toSet() }
    }
}
