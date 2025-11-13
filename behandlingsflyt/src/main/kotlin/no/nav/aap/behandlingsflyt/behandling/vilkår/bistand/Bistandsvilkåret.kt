package no.nav.aap.behandlingsflyt.behandling.vilkår.bistand

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode

class Bistandsvilkåret(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<BistandFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

    override fun vurder(grunnlag: BistandFaktagrunnlag) {
        val studentvurderingTidslinje = Tidslinje(
            Periode(grunnlag.vurderingsdato, grunnlag.sisteDagMedMuligYtelse),
            grunnlag.studentvurdering
        )
        val bistandvurderingTidslinje =
            grunnlag.bistandGrunnlag?.somBistandsvurderingstidslinje(grunnlag.sisteDagMedMuligYtelse).orEmpty()

        val tidslinje = studentvurderingTidslinje.kombiner(
            bistandvurderingTidslinje,
            JoinStyle.OUTER_JOIN { periode, studentSeg, bistandSeg ->
                Segment(periode, Pair(studentSeg?.verdi, bistandSeg?.verdi))
            }).mapValue { (studentvurdering, bistandsvurdering) ->
            opprettVilkårsvurdering(
                studentvurdering,
                bistandsvurdering,
                grunnlag
            )
        }

        vilkår.leggTilVurderinger(tidslinje)
    }

    private fun opprettVilkårsvurdering(
        studentvurdering: StudentVurdering?,
        bistandsvurdering: Bistandsvurdering?,
        grunnlag: BistandFaktagrunnlag
    ): Vilkårsvurdering {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak? = null

        if (studentvurdering?.erOppfylt() == true) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.STUDENT
        } else if (bistandsvurdering?.erBehovForBistand() == true) {
            utfall = Utfall.OPPFYLT
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
        }

        return Vilkårsvurdering(
            utfall = utfall,
            begrunnelse = null,
            innvilgelsesårsak = innvilgelsesårsak,
            avslagsårsak = avslagsårsak,
            faktagrunnlag = grunnlag,
            manuellVurdering = true,
        )
    }
}
