package no.nav.aap.behandlingsflyt.steg.institusjon

import java.time.LocalDate
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.institusjonsopphold.Institusjonsopphold
import no.nav.aap.komponenter.tidslinje.filterNotNull
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.misc.Vilkårsvurderer
import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.vilkårsresultat.Vilkårtype

class StraffegjennomføringGrunnlag(
    val institusjonsopphold: List<Institusjonsopphold>,
    val vurderFra: LocalDate,
) : Faktagrunnlag

class StraffegjennomføringVilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<StraffegjennomføringGrunnlag> {
    private val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.STRAFFEGJENNOMFØRING)

    override fun vurder(grunnlag: StraffegjennomføringGrunnlag) {
        val utgangspunkt = tidslinjeOf(
            Periode(grunnlag.vurderFra, Tid.MAKS) to Vilkårsvurdering(
                utfall = Utfall.OPPFYLT,
                begrunnelse = null,
                manuellVurdering = false,
                faktagrunnlag = grunnlag,
            )
        )

        val saksbehandlersVurdering = grunnlag.institusjonsopphold
            .somTidslinje { it.periode }
            .map { it.soning }
            .filterNotNull()
            .map {
                when {
                    it.soner && it.girOpphør ->
                        Vilkårsvurdering(
                            utfall = Utfall.IKKE_OPPFYLT,
                            begrunnelse = null, /* burde være mulig å fiske ut begrunnelsen ... */
                            avslagsårsak = Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING,
                            manuellVurdering = true,
                            faktagrunnlag = grunnlag,
                        )

                    else ->
                        Vilkårsvurdering(
                            utfall = Utfall.OPPFYLT,
                            begrunnelse = null, /* burde være mulig å fiske ut begrunnelsen ... */
                            manuellVurdering = true,
                            faktagrunnlag = grunnlag,
                        )
                }
            }

        vilkår.leggTilVurderinger(utgangspunkt.mergePrioriterHøyre(saksbehandlersVurdering))
    }
}