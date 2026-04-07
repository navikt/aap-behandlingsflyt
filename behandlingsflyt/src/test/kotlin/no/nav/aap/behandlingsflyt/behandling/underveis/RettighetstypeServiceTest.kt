package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.help.genererVilkårsresultat
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryRettighetstypeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RettighetstypeServiceTest {
    
    @Test
    fun `Kan sjekke om bruker har rett i en gitt periode`() {
        val rettighetsperiode = Periode(1 januar 2020, Tid.MAKS)

        val nå = 1 januar 2021

        val fulltAvslag = genererVilkårsresultat(
            rettighetsperiode,
            bistandVilkåret = Vilkår(
                Vilkårtype.BISTANDSVILKÅRET, setOf(
                    Vilkårsperiode(
                        rettighetsperiode,
                        Utfall.IKKE_OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
                    )
                )
            )
        )

        val avslagPåAlderIGår = genererVilkårsresultat(
            rettighetsperiode,
            aldersVilkåret = Vilkår(
                Vilkårtype.ALDERSVILKÅRET, setOf(
                    Vilkårsperiode(
                        Periode(rettighetsperiode.fom, nå),
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    ),
                    Vilkårsperiode(
                        Periode(nå.plusDays(1), Tid.MAKS),
                        Utfall.IKKE_OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = Avslagsårsak.BRUKER_OVER_67
                    )
                )
            )
        )

        val avslagPåAlderIDag = genererVilkårsresultat(
            rettighetsperiode,
            aldersVilkåret = Vilkår(
                Vilkårtype.ALDERSVILKÅRET, setOf(
                    Vilkårsperiode(
                        Periode(rettighetsperiode.fom, nå.minusDays(1)),
                        Utfall.OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null
                    ),
                    Vilkårsperiode(
                        Periode(nå, Tid.MAKS),
                        Utfall.IKKE_OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = Avslagsårsak.BRUKER_OVER_67
                    )
                )
            )
        )

        InMemoryVilkårsresultatRepository.lagre(BehandlingId(1), fulltAvslag)
        InMemoryVilkårsresultatRepository.lagre(BehandlingId(2), avslagPåAlderIGår)
        InMemoryVilkårsresultatRepository.lagre(BehandlingId(3), avslagPåAlderIDag)

        val rettighetstypeService = RettighetstypeService(
            InMemoryRettighetstypeRepository,
            InMemoryVilkårsresultatRepository,
            InMemoryUnderveisRepository
        )


        assertThat(
            rettighetstypeService.harRettInnenforPeriode(
                BehandlingId(1),
                Periode(
                    nå,
                    rettighetsperiode.tom
                )
            )
        ).isFalse()
        
        
        assertThat(
            rettighetstypeService.harRettInnenforPeriode(
                 BehandlingId(2),
                Periode(
                    nå,
                    rettighetsperiode.tom
                )
            )
        ).isTrue()

        assertThat(
            rettighetstypeService.harRettInnenforPeriode(
                BehandlingId(3),
                Periode(
                    nå,
                    rettighetsperiode.tom
                )
            )
        ).isFalse()
    }

}