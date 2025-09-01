package no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode


class OvergangUføreVilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<OvergangUføreFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.finnVilkår(Vilkårtype.OVERGANGUFØREVILKÅRET)
    override fun vurder(grunnlag: OvergangUføreFaktagrunnlag) {

        val overgangUføreVurderinger = grunnlag.vurderinger

        val overgangUføreTidslinje = overgangUføreVurderinger
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
            .fold(Tidslinje<OvergangUføreVurdering>()) { t1, t2 ->
                t1.kombiner(t2, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }

        val tidslinje = overgangUføreTidslinje.mapValue { overganguførevurdering ->
            opprettVilkårsvurdering(
                overganguførevurdering,
                grunnlag
            )
        }
        vilkår.leggTilVurderinger(tidslinje)
    }

    private fun opprettVilkårsvurdering(
        overgangUføreVurdering: OvergangUføreVurdering?,
        grunnlag: OvergangUføreFaktagrunnlag
    ): Vilkårsvurdering {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak? = null

        if (overgangUføreVurdering == null) {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON
        } else if (overgangUføreVurdering.virkningsDato != null && overgangUføreVurdering.brukerSoktUforetrygd && overgangUføreVurdering.brukerVedtakUforetrygd == UføreSøknadVedtak.NEI.verdi && overgangUføreVurdering.brukerRettPaaAAP == true) {

            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON
        }

        return Vilkårsvurdering(
            Vilkårsperiode(
                periode = Periode(grunnlag.vurderingsdato, grunnlag.vurderingsdato.plusMonths(8)),
                utfall = utfall,
                begrunnelse = null,
                innvilgelsesårsak = innvilgelsesårsak,
                avslagsårsak = avslagsårsak,
                faktagrunnlag = grunnlag,
                manuellVurdering = false
            )
        )
    }

    enum class UføreSøknadVedtak(val verdi: String) {
        JA_AVSLAG("JA_AVSLAG"), JA_GRADERT("JA_GRADERT"), JA_FULL("JA_FULL"), NEI("NEI")
    }
}
