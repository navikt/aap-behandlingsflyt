package no.nav.aap.behandlingsflyt.vilkår.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.vilkår.VurderingsResultat
import no.nav.aap.verdityper.Periode

class SykepengerErstatningVilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<SykepengerErstatningFaktagrunnlag> {
    private val vilkår: Vilkår

    init {
        this.vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING)
    }

    override fun vurder(grunnlag: SykepengerErstatningFaktagrunnlag): VurderingsResultat {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null

        val sykdomsvurdering = grunnlag.vurdering

        if (sykdomsvurdering.harRettPå == true) {
            utfall = Utfall.OPPFYLT
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO noe mer rett
        }

        return lagre(grunnlag, VurderingsResultat(
            utfall = utfall,
            avslagsårsak,
            null
        ))
    }

    private fun lagre(grunnlag: SykepengerErstatningFaktagrunnlag, vurderingsResultat: VurderingsResultat): VurderingsResultat {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                Periode(grunnlag.vurderingsdato, grunnlag.sisteDagMedMuligYtelse),
                vurderingsResultat.utfall,
                false,
                null,
                null,
                vurderingsResultat.avslagsårsak,
                grunnlag,
                vurderingsResultat.versjon()
            )
        )

        return vurderingsResultat
    }

}
