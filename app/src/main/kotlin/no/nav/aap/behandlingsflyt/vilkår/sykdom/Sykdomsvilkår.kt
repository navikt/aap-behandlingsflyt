package no.nav.aap.behandlingsflyt.vilkår.sykdom

import no.nav.aap.behandlingsflyt.vilkår.Avslagsårsak
import no.nav.aap.behandlingsflyt.vilkår.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.vilkår.Vilkår
import no.nav.aap.behandlingsflyt.vilkår.Vilkårsperiode
import no.nav.aap.behandlingsflyt.vilkår.Vilkårsresultat
import no.nav.aap.behandlingsflyt.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.vilkår.VurderingsResultat
import no.nav.aap.verdityper.Periode

class Sykdomsvilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<SykdomsFaktagrunnlag> {
    private val vilkår: Vilkår

    init {
        this.vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
    }

    override fun vurder(grunnlag: SykdomsFaktagrunnlag): VurderingsResultat {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak? = null

        val sykdomsvurdering = grunnlag.sykdomsvurdering

        if (grunnlag.studentvurdering?.oppfyller11_14 == true) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.STUDENT
        } else if (sykdomsvurdering?.erSkadeSykdomEllerLyteVesentligdel == true && sykdomsvurdering.erNedsettelseIArbeidsevneHøyereEnnNedreGrense == true) {
            utfall = Utfall.OPPFYLT
            val yrkesskadevurdering = grunnlag.yrkesskadevurdering
            if (yrkesskadevurdering != null && yrkesskadevurdering.erÅrsakssammenheng) {
                innvilgelsesårsak = Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG
            }
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = if (sykdomsvurdering?.erSkadeSykdomEllerLyteVesentligdel == false) {
                Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL
            } else if (sykdomsvurdering?.erNedsettelseIArbeidsevneHøyereEnnNedreGrense == false) {
                Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE
            } else {
                Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO noe mer rett
            }
        }

        return lagre(
            grunnlag,
            VurderingsResultat(
                utfall = utfall,
                avslagsårsak = avslagsårsak,
                innvilgelsesårsak = innvilgelsesårsak
            )
        )
    }

    private fun lagre(grunnlag: SykdomsFaktagrunnlag, vurderingsResultat: VurderingsResultat): VurderingsResultat {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                Periode(grunnlag.vurderingsdato, grunnlag.sisteDagMedMuligYtelse),
                vurderingsResultat.utfall,
                false,
                null,
                vurderingsResultat.innvilgelsesårsak,
                vurderingsResultat.avslagsårsak,
                grunnlag,
                vurderingsResultat.versjon()
            )
        )

        return vurderingsResultat
    }

}
