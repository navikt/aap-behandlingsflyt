package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.help.genererVilkårsresultat
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
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

        val (_, behandling1) = opprettInMemorySakOgBehandling()
        val (_, behandling2) = opprettInMemorySakOgBehandling()
        val (_, behandling3) = opprettInMemorySakOgBehandling()
        InMemoryVilkårsresultatRepository.lagre(behandling1.id, fulltAvslag)
        InMemoryVilkårsresultatRepository.lagre(behandling2.id, avslagPåAlderIGår)
        InMemoryVilkårsresultatRepository.lagre(behandling3.id, avslagPåAlderIDag)

        val rettighetstypeService = RettighetstypeService(inMemoryRepositoryProvider, minimalGatewayProvider {  })


        assertThat(
            rettighetstypeService.harRettInnenforPeriode(
                behandling1.id,
                Periode(
                    nå,
                    rettighetsperiode.tom
                )
            )
        ).isFalse()
        
        
        assertThat(
            rettighetstypeService.harRettInnenforPeriode(
                 behandling2.id,
                Periode(
                    nå,
                    rettighetsperiode.tom
                )
            )
        ).isTrue()

        assertThat(
            rettighetstypeService.harRettInnenforPeriode(
                behandling3.id,
                Periode(
                    nå,
                    rettighetsperiode.tom
                )
            )
        ).isFalse()
    }

}