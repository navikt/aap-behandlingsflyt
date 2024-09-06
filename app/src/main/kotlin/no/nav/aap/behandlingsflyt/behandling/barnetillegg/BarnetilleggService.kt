package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnetilleggService(
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val barnRepository: BarnRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository
) {
    fun beregn(behandlingId: BehandlingId): Tidslinje<RettTilBarnetillegg> {
        val sak = sakOgBehandlingService.hentSakFor(behandlingId)
        var resultat: Tidslinje<RettTilBarnetillegg> =
            Tidslinje(listOf(Segment(sak.rettighetsperiode, RettTilBarnetillegg())))

        if (skalIkkeBeregneBarnetilegg(behandlingId)) {
            return resultat
        }

        val personopplysningerGrunnlag = requireNotNull(personopplysningRepository.hentHvisEksisterer(behandlingId))

        val barnGrunnlag = barnRepository.hent(behandlingId)
        val folkeregisterBarn =
            barnGrunnlag.registerbarn?.identer?.map { ident -> mapTilBarn(ident, personopplysningerGrunnlag) }
                ?: emptyList()
        val folkeregisterBarnTidslinje = tilTidslinje(folkeregisterBarn)

        resultat =
            resultat.kombiner(folkeregisterBarnTidslinje, JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                val venstreVerdi = venstreSegment.verdi
                if (høyreSegment?.verdi != null) {
                    venstreVerdi.leggTilFolkeregisterBarn(høyreSegment.verdi)
                }
                Segment(periode, venstreVerdi)
            })

        val oppgittBarn =
            barnGrunnlag.oppgittBarn?.identer?.map { ident -> mapTilBarn(ident, personopplysningerGrunnlag) }
                ?: emptyList()
        val oppgittBarnTidslinje = tilTidslinje(oppgittBarn)
        resultat =
            resultat.kombiner(oppgittBarnTidslinje, JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                val venstreVerdi = venstreSegment.verdi
                if (høyreSegment?.verdi != null) {
                    venstreVerdi.leggTilOppgitteBarn(høyreSegment.verdi)
                }
                Segment(periode, venstreVerdi)
            })

        //hent saksbehanler else request saksbehandler
        val vurderteBarn = barnGrunnlag.vurderteBarn?.barn ?: emptyList()

        for (barn in vurderteBarn) {
            resultat = resultat.kombiner(
                Tidslinje(barn.vurderinger.map { Segment(it.periode, it.harForeldreAnsvar) }),
                JoinStyle.CROSS_JOIN { periode, venstreSegment, høyreSegment ->
                    val høyreVerdi = høyreSegment?.verdi
                    val nyVenstreVerdi = venstreSegment?.verdi ?: RettTilBarnetillegg()
                    if (høyreVerdi != null) {
                        if (høyreVerdi) {
                            nyVenstreVerdi.godkjenteBarn(setOf(barn.ident))
                        } else {
                            nyVenstreVerdi.underkjenteBarn(setOf(barn.ident))
                        }
                    }

                    Segment(periode, nyVenstreVerdi)
                })
        }

        return resultat.kryss(sak.rettighetsperiode)
    }

    private fun skalIkkeBeregneBarnetilegg(behandlingId: BehandlingId): Boolean {
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

        return !(sykdomsvilkåret.harPerioderSomErOppfylt() && bistandsvilkåret.harPerioderSomErOppfylt())
    }

    private fun mapTilBarn(ident: Ident, personopplysningerGrunnlag: PersonopplysningGrunnlag): Barn {
        val personopplysning1 = personopplysningerGrunnlag.relatertePersonopplysninger?.personopplysninger?.single {
            it.gjelderForIdent(ident)
        }
        val personopplysning =
            requireNotNull(personopplysning1)
        return Barn(personopplysning.ident(), personopplysning.fødselsdato, personopplysning.dødsdato)
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
                JoinStyle.CROSS_JOIN { periode, venstreSegment, høyreSegment ->
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