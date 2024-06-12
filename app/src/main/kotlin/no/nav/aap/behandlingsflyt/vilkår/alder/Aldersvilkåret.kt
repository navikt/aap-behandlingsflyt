package no.nav.aap.behandlingsflyt.vilkår.alder

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.vilkår.VurderingsResultat
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Periode
import java.time.LocalDate

class Aldersvilkåret(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<Aldersgrunnlag> {
    private val vilkår: Vilkår

    init {
        this.vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.ALDERSVILKÅRET)
    }

    // TODO Det må avklares hva som er riktig adferd dersom bruker søker før fylte 18
    override fun vurder(grunnlag: Aldersgrunnlag): VurderingsResultat {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null

        val alderPåSøknadsdato = grunnlag.alderPåSøknadsdato()

        if (alderPåSøknadsdato < 18) {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.BRUKER_UNDER_18
            return lagre(
                grunnlag, VurderingsResultat(
                    utfall = utfall,
                    avslagsårsak = avslagsårsak,
                    innvilgelsesårsak = null
                )
            )
        } else if (alderPåSøknadsdato >= 67) {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.BRUKER_OVER_67
            return lagre(
                grunnlag, VurderingsResultat(
                    utfall = utfall,
                    avslagsårsak = avslagsårsak,
                    innvilgelsesårsak = null
                )
            )
        }

        val alderstidslinje = Tidslinje(grunnlag.periode, Utfall.OPPFYLT).kombiner(Tidslinje(
            Periode(
                grunnlag.sisteDagMedYtelse(),
                LocalDate.MAX
            ), Utfall.IKKE_OPPFYLT
        ), JoinStyle.LEFT_JOIN { periode, venstre, høyre ->
            Segment(
                periode, VurderingMedÅrsak(
                    høyre?.verdi ?: venstre.verdi, utledÅrsak(høyre)
                )
            )
        })

        for (segment in alderstidslinje) {
            lagre(
                grunnlag, VurderingsResultat(
                    utfall = segment.verdi.utfall,
                    avslagsårsak = segment.verdi.avslagsårsak,
                    innvilgelsesårsak = null
                )
            )
        }

        return lagre(
            grunnlag, VurderingsResultat(
                utfall = Utfall.OPPFYLT,
                avslagsårsak = avslagsårsak,
                innvilgelsesårsak = null
            )
        )
    }

    private fun utledÅrsak(høyre: Segment<Utfall>?): Avslagsårsak? {
        return if (høyre?.verdi == Utfall.IKKE_OPPFYLT) {
            Avslagsårsak.BRUKER_OVER_67
        } else {
            null
        }
    }

    private fun lagre(grunnlag: Aldersgrunnlag, vurderingsResultat: VurderingsResultat): VurderingsResultat {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                periode = grunnlag.periode,
                utfall = vurderingsResultat.utfall,
                avslagsårsak = vurderingsResultat.avslagsårsak,
                begrunnelse = null,
                faktagrunnlag = grunnlag,
                versjon = vurderingsResultat.versjon()
            )
        )

        return vurderingsResultat
    }

}
