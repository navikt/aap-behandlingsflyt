package no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode


class OvergangUføreVilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<OvergangUføreFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.finnVilkår(Vilkårtype.OVERGANGUFØREVILKÅRET)
    override fun vurder(grunnlag: OvergangUføreFaktagrunnlag) {

        val overgangUføreVurderinger = grunnlag.vurderinger
        val sorterteVurderinger = overgangUføreVurderinger.sortedBy { it.opprettet }

        val overgangUføreTidslinje = sorterteVurderinger.map { vurdering ->
            Tidslinje(
                Periode(
                    fom = listOfNotNull(
                        vurdering.vurderingenGjelderFra,
                        vurdering.virkningsdato,
                        grunnlag.rettighetsperiode.fom,
                    ).max(),
                    tom = grunnlag.rettighetsperiode.tom
                ), vurdering
            )
        }.fold(Tidslinje<OvergangUføreVurdering>()) { t1, t2 ->
            t1.kombiner(t2, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }


        val tidslinje = overgangUføreTidslinje.mapValue { overganguførevurdering ->
            opprettVilkårsvurdering(
                overganguførevurdering, grunnlag
            )
        }
        val oppfyltIkkeOppfyltTidslinje = Tidslinje(tidslinje.mapValue { it.erOppfylt() }.komprimer().map{ begrensSegment(it) })
        val begrensetTidslinje: Tidslinje<Vilkårsvurdering> = tidslinje.innerJoin(oppfyltIkkeOppfyltTidslinje) { _, vilårsvurdering, _ ->
            vilårsvurdering
        }

        vilkår.leggTilVurderinger(begrensetTidslinje)
    }

    private fun begrensSegment(segment: Segment<Boolean>): Segment<Boolean> {
        val maksDato = segment.fom().plusMonths(8).minusDays(1)
        val erOppfylt = segment.verdi
        return if (!erOppfylt || segment.tom().isBefore(maksDato)) {
            segment
        } else {
            Segment(Periode(segment.fom(), maksDato), segment.verdi)
        }
    }

    private fun opprettVilkårsvurdering(
        overgangUføreVurdering: OvergangUføreVurdering?, grunnlag: OvergangUføreFaktagrunnlag
    ): Vilkårsvurdering {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak? = null

        if (overgangUføreVurdering == null) {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE
        } else if (overgangUføreVurdering.harRettPåAAPMedOvergangUføre()) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE
        }

        return Vilkårsvurdering(
            utfall = utfall,
            begrunnelse = null,
            innvilgelsesårsak = innvilgelsesårsak,
            avslagsårsak = avslagsårsak,
            faktagrunnlag = grunnlag,
            manuellVurdering = false
        )
    }

    enum class UføreSøknadVedtak(val verdi: String) {
        JA_AVSLAG("JA_AVSLAG"), JA_GRADERT("JA_GRADERT"), JA_FULL("JA_FULL"), NEI("NEI")
    }
}
