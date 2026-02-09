package no.nav.aap.behandlingsflyt.behandling.vilkår.student

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

class StudentVilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<StudentFaktagrunnlag> {
    companion object {
        fun utledVarighetSluttdato(fraDato: LocalDate) = fraDato.plusMonths(6).minusDays(1)
    }

    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.STUDENT)

    override fun vurder(grunnlag: StudentFaktagrunnlag) {
        val studentTidslinje = grunnlag.studentGrunnlag?.somStudenttidslinje(grunnlag.rettighetsperiode).orEmpty()

        // Varighet er utelukkende bestemt av datoen studiet ble avbrutt + 6 måneder
        // Overlappende varighetsperioder vil dermed kunne gi en sammenhengende varighet på mer enn 6 måneder
        val varighetsTidslinje =
            grunnlag.studentGrunnlag?.gjeldendeStudentvurderinger(grunnlag.rettighetsperiode).orEmpty()
                .filter { it.avbruttStudieDato != null }.sortedBy { it.avbruttStudieDato }
                .somTidslinje { Periode(it.avbruttStudieDato!!, utledVarighetSluttdato(it.avbruttStudieDato)) }
                .mapValue { true }

        vilkår.leggTilIkkeVurdertPeriode(grunnlag.rettighetsperiode) // Resetter vilkårstidslinjen
        vilkår.leggTilVurderinger(
            studentTidslinje.leftJoin(varighetsTidslinje) { studentvurdering, varighetOk ->
                if (studentvurdering.erOppfylt()) {
                    if (varighetOk == true) {
                        Vilkårsvurdering(
                            utfall = Utfall.OPPFYLT,
                            begrunnelse = studentvurdering.begrunnelse,
                            faktagrunnlag = grunnlag,
                            manuellVurdering = true
                        )
                    } else {
                        Vilkårsvurdering(
                            utfall = Utfall.IKKE_OPPFYLT,
                            avslagsårsak = Avslagsårsak.VARIGHET_OVERSKREDET_STUDENT,
                            begrunnelse = "Varighet overskredet.",
                            faktagrunnlag = grunnlag,
                            manuellVurdering = false
                        )
                    }
                } else {
                    Vilkårsvurdering(
                        utfall = Utfall.IKKE_OPPFYLT,
                        avslagsårsak = Avslagsårsak.IKKE_RETT_PA_STUDENT,
                        begrunnelse = studentvurdering.begrunnelse,
                        faktagrunnlag = grunnlag,
                        manuellVurdering = true
                    )
                }
            })

    }
}
