package no.nav.aap.behandlingsflyt.behandling.vilkår.oppholdskrav

import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class OppholdskravvilkårGrunnlag(
    val oppholdskravGrunnlag: OppholdskravGrunnlag?,
    val vurderFra: LocalDate,
) : Faktagrunnlag

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