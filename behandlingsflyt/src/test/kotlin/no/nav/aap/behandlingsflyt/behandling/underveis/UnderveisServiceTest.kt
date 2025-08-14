package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.tomUnderveisInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.sql.DataSource

class UnderveisServiceTest {
    private val dataSource: DataSource = MockDataSource()
    private val kvoter = tomKvoter

    @Test
    fun `skal vurdere alle reglene`() {
        dataSource.transaction { connection ->
            val underveisService = UnderveisService(
                postgresRepositoryRegistry.provider(connection),
                createGatewayProvider {
                    register<FakeUnleash>()
                }
            )
            val søknadsdato = LocalDate.now().minusDays(29)
            val periode = Periode(søknadsdato, søknadsdato.plusYears(3))
            val aldersVilkåret =
                Vilkår(
                    Vilkårtype.ALDERSVILKÅRET, setOf(
                        Vilkårsperiode(
                            periode,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null
                        )
                    )
                )
            val lovvalgVilkåret =
                Vilkår(
                    Vilkårtype.LOVVALG, setOf(
                        Vilkårsperiode(
                            periode,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null
                        )
                    )
                )
            val sykdomsVilkåret =
                Vilkår(
                    Vilkårtype.SYKDOMSVILKÅRET, setOf(
                        Vilkårsperiode(
                            periode,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null
                        )
                    )
                )
            val medlemskapVilkåret =
                Vilkår(
                    Vilkårtype.MEDLEMSKAP, setOf(
                        Vilkårsperiode(
                            periode,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null
                        )
                    )
                )
            val bistandVilkåret =
                Vilkår(
                    Vilkårtype.BISTANDSVILKÅRET, setOf(
                        Vilkårsperiode(
                            periode,
                            Utfall.OPPFYLT,
                            false,
                            null,
                            faktagrunnlag = null
                        )
                    )
                )
            val relevanteVilkår = listOf(aldersVilkåret, lovvalgVilkåret, bistandVilkåret, medlemskapVilkåret, sykdomsVilkåret)
            val input = tomUnderveisInput(
                rettighetsperiode = periode,
                vilkårsresultat = Vilkårsresultat(null, relevanteVilkår),
                opptrappingPerioder = listOf(Periode(søknadsdato.plusYears(2), søknadsdato.plusYears(3))),
                kvoter = kvoter
            )

            val vurderingTidslinje = underveisService.vurderRegler(input)

            assertThat(vurderingTidslinje).isNotEmpty()
        }
    }
}
