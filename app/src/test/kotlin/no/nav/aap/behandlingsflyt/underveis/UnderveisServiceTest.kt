package no.nav.aap.behandlingsflyt.underveis

import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtestdata.MockDataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.PliktkortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.vilkårsresultat.Vilkårtype
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
                UnderveisService(VilkårsresultatRepository(connection), PliktkortRepository(connection), UnderveisRepository(connection))
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
