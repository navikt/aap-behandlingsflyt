package no.nav.aap.behandlingsflyt.behandling.vilkår.overgangarbeid

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode


class OvergangArbeidVilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<OvergangArbeidFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.finnVilkår(Vilkårtype.OVERGANGARBEIDVILKÅRET)
    override fun vurder(grunnlag: OvergangArbeidFaktagrunnlag) {
        val overgangArbeidVurderinger = grunnlag.vurderinger

        val overgangArbeidTidslinje = overgangArbeidVurderinger
            .sortedBy { it.opprettet }
            .map { vurdering ->
                Tidslinje(
                    Periode(
                        fom = vurdering.vurderingenGjelderFra ?: grunnlag.vurderingsdato,
                        tom = grunnlag.sisteDagMedMuligYtelse
                    ),
                    vurdering
                )
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
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
        } else if (overgangArbeidVurdering.virkningsDato != null &&
            overgangArbeidVurdering.brukerRettPaaAAP == true
        ) {

            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.ARBEIDSSØKER
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
        }

        return Vilkårsvurdering(
            Vilkårsperiode(
                periode = Periode(grunnlag.vurderingsdato, grunnlag.vurderingsdato.plusMonths(6)),
                utfall = utfall,
                begrunnelse = null,
                innvilgelsesårsak = innvilgelsesårsak,
                avslagsårsak = avslagsårsak,
                faktagrunnlag = grunnlag,
                manuellVurdering = false
            )
        )
    }
}
