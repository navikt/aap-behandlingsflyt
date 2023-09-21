package no.nav.aap.domene.vilkår.sykdom

import no.nav.aap.domene.Periode
import no.nav.aap.domene.behandling.Avslagsårsak
import no.nav.aap.domene.behandling.TomtBeslutningstre
import no.nav.aap.domene.behandling.Utfall
import no.nav.aap.domene.behandling.Vilkår
import no.nav.aap.domene.behandling.Vilkårsperiode
import no.nav.aap.domene.behandling.Vilkårstype
import no.nav.aap.domene.vilkår.Vilkårsvurderer
import no.nav.aap.domene.vilkår.VurderingsResultat

class Sykdomsvilkår(val vilkår: Vilkår) : Vilkårsvurderer<SykdomsFaktagrunnlag> {
    init {
        require(vilkår.type == Vilkårstype.SYKDOMSVILKÅRET) { "${vilkår.type} er ikke SYKDOMSVILKÅRET" }
    }

    override fun vurder(grunnlag: SykdomsFaktagrunnlag): VurderingsResultat {
        val utfall: Utfall
        var avslagsårsak : Avslagsårsak? = null

        val sykdomsvurdering = grunnlag.sykdomsvurdering

        if (sykdomsvurdering.erSkadeSykdomEllerLyteVesentligdel && sykdomsvurdering.erNedsettelseIArbeidsevneHøyereEnnNedreGrense == true) {
            utfall = Utfall.OPPFYLT
            // TODO: Legge til innvilget etter (f.eks YS)
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO noe mer rett
        }

        return lagre(grunnlag, VurderingsResultat(utfall = utfall, avslagsårsak, TomtBeslutningstre()))
    }

    private fun lagre(grunnlag: SykdomsFaktagrunnlag, vurderingsResultat: VurderingsResultat): VurderingsResultat {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                Periode(grunnlag.vurderingsdato, grunnlag.sisteDagMedMuligYtelse),
                vurderingsResultat.utfall,
                false,
                null,
                null,
                grunnlag,
                vurderingsResultat.beslutningstre
            )
        )

        return vurderingsResultat
    }

}
