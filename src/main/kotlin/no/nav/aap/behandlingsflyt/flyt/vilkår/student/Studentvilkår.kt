
package no.nav.aap.behandlingsflyt.flyt.vilkår.student

class Studentvilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<StudentFaktagrunnlag> {
    private val vilkår: Vilkår

    init {
        this.vilkår = vilkårsresultat.finnVilkår(Vilkårtype.STUDENTVILKÅRET)
    }

    override fun vurder(grunnlag: StudentFaktagrunnlag): VurderingsResultat {
        return lagre(
            grunnlag,
            VurderingsResultat(
                utfall = Utfall.OPPFYLT,
                avslagsårsak = null,
                innvilgelsesårsak = null,
                beslutningstre = TomtBeslutningstre()
            )
        )
    }

    private fun lagre(grunnlag: StudentFaktagrunnlag, vurderingsResultat: VurderingsResultat): VurderingsResultat {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                Periode(grunnlag.vurderingsdato, grunnlag.sisteDagMedMuligYtelse),
                vurderingsResultat.utfall,
                false,
                null,
                vurderingsResultat.innvilgelsesårsak,
                vurderingsResultat.avslagsårsak,
                grunnlag,
                vurderingsResultat.beslutningstre
            )
        )

        return vurderingsResultat
    }

}