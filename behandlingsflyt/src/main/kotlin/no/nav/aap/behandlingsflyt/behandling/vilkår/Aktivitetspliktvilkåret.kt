package no.nav.aap.behandlingsflyt.behandling.vilkår

import no.nav.aap.aktivitetsplikt.AktivitetspliktvilkåretGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.misc.Vilkårsvurderer
import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.vilkårsresultat.Vilkårtype

class Aktivitetspliktvilkåret(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<AktivitetspliktvilkåretGrunnlag> {
    private val vilkåret = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.AKTIVITETSPLIKT)

    override fun vurder(grunnlag: AktivitetspliktvilkåretGrunnlag) {
        val rettighetsperiode = Periode(grunnlag.vurderFra, Tid.MAKS)

        val utgangspunktOppfylt = tidslinjeOf(
            rettighetsperiode to Vilkårsvurdering(
                utfall = Utfall.OPPFYLT,
                manuellVurdering = false,
                begrunnelse = null,
                faktagrunnlag = grunnlag,
            )
        )

        val saksbehandlersVurderinger =
            grunnlag.aktivitetsplikt117grunnlag
                .tidslinje()
                .map {
                    Vilkårsvurdering(
                        utfall = when {
                            it.erOppfylt -> Utfall.OPPFYLT
                            else -> Utfall.IKKE_OPPFYLT
                        },
                        manuellVurdering = true,
                        begrunnelse = it.begrunnelse,
                        avslagsårsak = when (it.utfall) {
                            no.nav.aap.aktivitetsplikt.Utfall.STANS -> Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS
                            no.nav.aap.aktivitetsplikt.Utfall.OPPHØR -> Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR
                            null -> null
                        },
                        faktagrunnlag = grunnlag,
                    )
                }

        vilkåret.leggTilVurderinger(
            utgangspunktOppfylt.mergePrioriterHøyre(saksbehandlersVurderinger)
        )
    }
}