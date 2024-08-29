package no.nav.aap.behandlingsflyt.behandling.vilkår.alder

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.behandling.vilkår.VurderingsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import java.time.LocalDate

class Aldersvilkåret(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<Aldersgrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.ALDERSVILKÅRET)

    // TODO Det må avklares hva som er riktig adferd dersom bruker søker før fylte 18
    override fun vurder(grunnlag: Aldersgrunnlag) {
        val utfall: Utfall
        val avslagsårsak: Avslagsårsak?

        val alderPåSøknadsdato = grunnlag.alderPåSøknadsdato()

        if (alderPåSøknadsdato < 18) {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.BRUKER_UNDER_18
            lagre(
                grunnlag.periode, grunnlag, VurderingsResultat(
                    utfall = utfall,
                    avslagsårsak = avslagsårsak,
                    innvilgelsesårsak = null
                )
            )
            return
        } else if (alderPåSøknadsdato >= 67) {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.BRUKER_OVER_67
            lagre(
                grunnlag.periode, grunnlag, VurderingsResultat(
                    utfall = utfall,
                    avslagsårsak = avslagsårsak,
                    innvilgelsesårsak = null
                )
            )
            return
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
                segment.periode,
                grunnlag, VurderingsResultat(
                    utfall = segment.verdi.utfall,
                    avslagsårsak = segment.verdi.avslagsårsak,
                    innvilgelsesårsak = null
                )
            )
        }
    }

    private fun utledÅrsak(høyre: Segment<Utfall>?): Avslagsårsak? {
        return if (høyre?.verdi == Utfall.IKKE_OPPFYLT) {
            Avslagsårsak.BRUKER_OVER_67
        } else {
            null
        }
    }

    private fun lagre(
        periode: Periode,
        grunnlag: Aldersgrunnlag,
        vurderingsResultat: VurderingsResultat
    ) {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                periode = periode,
                utfall = vurderingsResultat.utfall,
                avslagsårsak = vurderingsResultat.avslagsårsak,
                begrunnelse = null,
                faktagrunnlag = grunnlag,
                versjon = vurderingsResultat.versjon()
            )
        )
    }

}
