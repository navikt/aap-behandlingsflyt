package no.nav.aap.behandlingsflyt.underveis

import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtestdata.MockDataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.PliktkortRepository
import no.nav.aap.behandlingsflyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.vilkår.Vilkår
import no.nav.aap.behandlingsflyt.vilkår.Vilkårsperiode
import no.nav.aap.behandlingsflyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.underveis.regler.UnderveisInput
import no.nav.aap.verdityper.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.sql.DataSource

class UnderveisServiceTest {

    private val dataSource: DataSource = MockDataSource()

    @Test
    fun `skal vurdere alle reglene`() {
        dataSource.transaction { connection ->
            val underveisService =
                UnderveisService(VilkårsresultatRepository(connection), PliktkortRepository(connection))
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
            val relevanteVilkår = listOf(aldersVilkåret, bistandVilkåret, sykdomsVilkåret)
            val input = UnderveisInput(
                førsteFastsatteDag = søknadsdato,
                relevanteVilkår = relevanteVilkår,
                opptrappingPerioder = listOf(Periode(søknadsdato.plusYears(2), søknadsdato.plusYears(3))),
                pliktkort = listOf()
            )

            val vurderingTidslinje = underveisService.vurderRegler(input)

            assertThat(vurderingTidslinje).isNotEmpty()
        }
    }
}
