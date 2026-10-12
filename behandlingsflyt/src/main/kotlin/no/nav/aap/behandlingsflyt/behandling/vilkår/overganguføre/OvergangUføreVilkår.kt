package no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre

import no.nav.aap.behandlingsflyt.behandling.vilkår.Varighetsvurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.mapMedDatoTilDatoVarighet
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import java.time.LocalDate

object OvergangUføreVilkår : Vilkårsvurderer<OvergangUføreFaktagrunnlag> {
    override val vilkårtype = Vilkårtype.OVERGANGUFØREVILKÅRET

    override fun vurder(faktagrunnlag: OvergangUføreFaktagrunnlag): Tidslinje<Vilkårsvurdering> {
        return faktagrunnlag.overgangUføreGrunnlag
            ?.somOvergangUforevurderingstidslinje()
            .orEmpty()
            .mapMedDatoTilDatoVarighet(
                harBegrensetVarighet = { it.harRettPåAAPMedOvergangUføre() },
                varighet = {
                    /* Fra lovteksten § 11-18:
                             * > Det kan gis arbeidsavklaringspenger i inntil åtte måneder
                             * > når medlemmet skal vurderes for uføretrygd.
                             *
                             * Dagens praksis i Arena er dato-til-dato, men regelspesifiseringen gir ingen spesifikasjon.
                             */
                    utledVarighetSluttdato(it)
                },
            ) { varighetsvurdering, vurdering ->
                when (varighetsvurdering) {
                    Varighetsvurdering.VARIGHET_OK ->
                        if (vurdering.harRettPåAAPMedOvergangUføre()) {
                            Vilkårsvurdering(
                                utfall = Utfall.OPPFYLT,
                                begrunnelse = vurdering.begrunnelse,
                                innvilgelsesårsak = null,
                                faktagrunnlag = faktagrunnlag,
                                manuellVurdering = true,
                            )
                        } else {
                            Vilkårsvurdering(
                                utfall = Utfall.IKKE_OPPFYLT,
                                begrunnelse = vurdering.begrunnelse,
                                innvilgelsesårsak = null,
                                avslagsårsak = Avslagsårsak.IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE,
                                faktagrunnlag = faktagrunnlag,
                                manuellVurdering = true,
                            )
                        }

                    Varighetsvurdering.VARIGHET_OVERSKREDET ->
                        Vilkårsvurdering(
                            utfall = Utfall.IKKE_OPPFYLT,
                            begrunnelse = null,
                            innvilgelsesårsak = null,
                            avslagsårsak = Avslagsårsak.VARIGHET_OVERSKREDET_OVERGANG_UFORE,
                            faktagrunnlag = faktagrunnlag,
                            manuellVurdering = false,
                        )
                }
            }
    }

    private fun utledVarighetSluttdato(fraDato: LocalDate): LocalDate = fraDato.plusMonths(8).minusDays(1)
}
