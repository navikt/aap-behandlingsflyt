package no.nav.aap.behandlingsflyt.behandling.vilkår.straffegjennomføring

import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.filterNotNull
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

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
