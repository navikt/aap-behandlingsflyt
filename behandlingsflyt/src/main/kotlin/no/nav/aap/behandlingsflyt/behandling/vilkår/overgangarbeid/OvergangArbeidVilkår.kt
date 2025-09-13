package no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid


class OvergangArbeidVilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<OvergangArbeidFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGARBEIDVILKÅRET)
    override fun vurder(grunnlag: OvergangArbeidFaktagrunnlag) {
        val overgangArbeidVurderinger = grunnlag.vurderinger

        val overgangArbeidTidslinje = overgangArbeidVurderinger
            .sortedBy { it.opprettet }
            .map { vurdering ->
                val fom = vurdering.vurderingenGjelderFra ?: grunnlag.vurderingsdato
                val tom = if (vurdering.brukerRettPåAAP == true)
                    /* Vilkåret har en begrensning på maks 6 måneder. Dette burde sjekkes på
                     * tvers av vurderinger. Men dette vil fungere i den vanlige casen. Eksempel
                     * på 6-månders-periode fra regelspesifiseringen: 01.02.23 - 31.07.23 */
                    fom.plusMonths(6).minusDays(1)
                else
                    Tid.MAKS

                Tidslinje(Periode(fom, tom), vurdering)
            }
            .fold(Tidslinje<OvergangArbeidVurdering>()) { t1, t2 ->
                t1.kombiner(t2, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }

        val tidslinje = overgangArbeidTidslinje.mapValue { overgangarbeidvurdering ->
            opprettVilkårsvurdering(
                overgangarbeidvurdering,
                grunnlag
            )
        }
        vilkår.leggTilVurderinger(tidslinje)
    }

    private fun opprettVilkårsvurdering(
        overgangArbeidVurdering: OvergangArbeidVurdering?,
        grunnlag: OvergangArbeidFaktagrunnlag
    ): Vilkårsvurdering {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak? = null

        if (overgangArbeidVurdering == null) {
            utfall = Utfall.IKKE_VURDERT
        } else if (
        /* Jeg forstår ikke hva denne sjekken på virknignsdato gjør. (peterbb) */
            overgangArbeidVurdering.virkningsdato != null &&
            overgangArbeidVurdering.brukerRettPåAAP == true
        ) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.ARBEIDSSØKER
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER
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
}
