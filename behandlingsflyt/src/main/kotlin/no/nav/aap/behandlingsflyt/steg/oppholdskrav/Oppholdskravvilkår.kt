package no.nav.aap.behandlingsflyt.steg.oppholdskrav

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.misc.Vilkårsvurderer
import no.nav.aap.oppholdskrav.OppholdskravvilkårGrunnlag
import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.vilkårsresultat.Vilkårtype

class Oppholdskravvilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<OppholdskravvilkårGrunnlag> {
    private val vilkåret = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.OPPHOLDSKRAV)

    override fun vurder(grunnlag: OppholdskravvilkårGrunnlag) {
        val rettighetsperiode = Periode(grunnlag.vurderFra, Tid.MAKS)

        val utgangspunktOppfylt = tidslinjeOf(
            rettighetsperiode to Vilkårsvurdering(
                utfall = Utfall.OPPFYLT,
                begrunnelse = null,
                faktagrunnlag = grunnlag,
                manuellVurdering = false,
            )
        )

        val saksbehandlersVurdering = grunnlag.oppholdskravGrunnlag
            ?.tidslinje()
            .orEmpty()
            .map {
                Vilkårsvurdering(
                    utfall = if (it.oppfylt) Utfall.OPPFYLT else Utfall.IKKE_OPPFYLT,
                    avslagsårsak = if (it.oppfylt) null else Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_STANS,
                    begrunnelse = it.begrunnelse,
                    faktagrunnlag = grunnlag,
                    manuellVurdering = true,
                )
            }

        vilkåret.leggTilVurderinger(
            utgangspunktOppfylt
                .mergePrioriterHøyre(saksbehandlersVurdering)
                .begrensetTil(rettighetsperiode)
        )
    }
}