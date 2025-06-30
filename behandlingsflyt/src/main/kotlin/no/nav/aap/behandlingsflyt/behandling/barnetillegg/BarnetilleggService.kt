package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

class BarnetilleggService(
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val barnRepository: BarnRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository
) {
    constructor(repositoryProvider: RepositoryProvider): this(
        sakOgBehandlingService = SakOgBehandlingService(repositoryProvider),
        barnRepository = repositoryProvider.provide(),
        personopplysningRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
    )

    fun beregn(behandlingId: BehandlingId): Tidslinje<RettTilBarnetillegg> {
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        var resultat: Tidslinje<RettTilBarnetillegg> =
            Tidslinje(listOf(Segment(sak.rettighetsperiode, RettTilBarnetillegg())))

        if (skalIkkeBeregneBarnetilegg(behandlingId)) {
            return resultat
        }

        val personopplysningerGrunnlag = requireNotNull(personopplysningRepository.hentHvisEksisterer(behandlingId))

        val barnGrunnlag = barnRepository.hentHvisEksisterer(behandlingId)
        if (barnGrunnlag == null) {
            return resultat
        }
        val folkeregisterBarn =
            barnGrunnlag.registerbarn?.identer?.mapNotNull { ident -> mapTilBarn(ident, personopplysningerGrunnlag) }
                ?: emptyList()
        val folkeregisterBarnTidslinje = tilTidslinje(folkeregisterBarn)

        resultat =
            resultat.kombiner(folkeregisterBarnTidslinje, JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                val venstreVerdi = venstreSegment.verdi.copy()
                if (høyreSegment?.verdi != null) {
                    venstreVerdi.leggTilFolkeregisterBarn(høyreSegment.verdi)
                }
                Segment(periode, venstreVerdi)
            })

        val vurderteBarn = barnGrunnlag.vurderteBarn?.barn ?: emptyList()
        val vurderteBarnIdenter = vurderteBarn.map { it.ident }
        val oppgittBarn =
            barnGrunnlag.oppgitteBarn?.identer?.mapNotNull { ident -> mapTilBarn(ident, personopplysningerGrunnlag) }
                ?.filterNot { vurderteBarnIdenter.contains(it.ident) }
                ?: emptyList()

        val oppgittBarnTidslinje = tilTidslinje(oppgittBarn)
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

    private fun skalIkkeBeregneBarnetilegg(behandlingId: BehandlingId): Boolean {
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        return !(sykdomsvilkåret.harPerioderSomErOppfylt() && bistandsvilkåret.harPerioderSomErOppfylt())
    }

    private fun mapTilBarn(ident: Ident, personopplysningerGrunnlag: PersonopplysningGrunnlag): Barn? {
        val personopplysning1 =
            personopplysningerGrunnlag.relatertePersonopplysninger?.personopplysninger?.singleOrNull() {
                it.gjelderForIdent(ident)
            }
        if (personopplysning1 == null) {
            return null
        }
        return Barn(personopplysning1.ident(), personopplysning1.fødselsdato, personopplysning1.dødsdato)
    }

    private fun tilTidslinje(barna: List<Barn>): Tidslinje<Set<Ident>> {
        var tidslinje: Tidslinje<MutableSet<Ident>> = Tidslinje()
        barna.map { barnet ->
            Segment(
                verdi = barnet.ident, periode = barnet.periodeMedRettTil()
            )
        }.forEach { segment ->
            tidslinje = tidslinje.kombiner(
                Tidslinje(listOf(segment)),
                JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
                    val verdi: MutableSet<Ident> = venstreSegment?.verdi ?: mutableSetOf()
                    if (høyreSegment?.verdi != null) {
                        verdi.add(høyreSegment.verdi)
                    }
                    Segment(periode, verdi)
                })
        }
        return tidslinje.mapValue { it.toSet() }
    }
}