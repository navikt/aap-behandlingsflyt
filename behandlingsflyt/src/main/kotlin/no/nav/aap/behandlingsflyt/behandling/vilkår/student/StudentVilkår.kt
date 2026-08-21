package no.nav.aap.behandlingsflyt.behandling.vilkår.student

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate


object StudentVilkår : Vilkårsvurderer<StudentFaktagrunnlag> {
    fun utledVarighetSluttdato(fraDato: LocalDate): LocalDate = fraDato.plusMonths(6).minusDays(1)

    override val vilkårtype: Vilkårtype = Vilkårtype.STUDENT

    override fun vurder(faktagrunnlag: StudentFaktagrunnlag): Tidslinje<Vilkårsvurdering> {
        val studentTidslinje =
            faktagrunnlag.studentGrunnlag?.somStudenttidslinje(faktagrunnlag.rettighetsperiode.tom).orEmpty()

        // Varighet er utelukkende bestemt av datoen studiet ble avbrutt + 6 måneder
        // Overlappende varighetsperioder vil dermed kunne gi en sammenhengende varighet på mer enn 6 måneder
        val varighetsTidslinje =
            faktagrunnlag.studentGrunnlag?.gjeldendeStudentvurderinger(faktagrunnlag.rettighetsperiode.tom).orEmpty()
                .filter { it.avbruttStudieDato != null }.sortedBy { it.avbruttStudieDato }
                .somTidslinje { Periode(it.avbruttStudieDato!!, utledVarighetSluttdato(it.avbruttStudieDato)) }
                .mapValue { true }

        return studentTidslinje.leftJoin(varighetsTidslinje) { studentvurdering, varighetOk ->
            if (studentvurdering.erOppfylt()) {
                if (varighetOk == true) {
                    Vilkårsvurdering(
                        utfall = Utfall.OPPFYLT,
                        begrunnelse = studentvurdering.begrunnelse,
                        faktagrunnlag = faktagrunnlag,
                        manuellVurdering = true
                    )
                } else {
                    Vilkårsvurdering(
                        utfall = Utfall.IKKE_OPPFYLT,
                        avslagsårsak = Avslagsårsak.VARIGHET_OVERSKREDET_STUDENT,
                        begrunnelse = "Varighet overskredet.",
                        faktagrunnlag = faktagrunnlag,
                        manuellVurdering = false
                    )
                }
            } else {
                Vilkårsvurdering(
                    utfall = Utfall.IKKE_OPPFYLT,
                    avslagsårsak = Avslagsårsak.IKKE_RETT_PA_STUDENT,
                    begrunnelse = studentvurdering.begrunnelse,
                    faktagrunnlag = faktagrunnlag,
                    manuellVurdering = true
                )
            }
        }

    }
}
