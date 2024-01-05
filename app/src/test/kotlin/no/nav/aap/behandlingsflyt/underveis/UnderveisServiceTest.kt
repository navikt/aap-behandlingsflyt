package no.nav.aap.behandlingsflyt.underveis

import no.nav.aap.behandlingsflyt.verdityper.Periode
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.MockConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.PliktkortRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkår
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsperiode
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.underveis.regler.UnderveisInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UnderveisServiceTest {

    private val connection = DBConnection(MockConnection())
    private val underveisService =
        UnderveisService(VilkårsresultatRepository(connection), PliktkortRepository(connection))

    @Test
    fun `skal vurdere alle reglene`() {
        val søknadsdato = LocalDate.now().minusDays(29)
        val periode = Periode(søknadsdato, søknadsdato.plusYears(3))
        val aldersVilkåret =
            Vilkår(Vilkårtype.ALDERSVILKÅRET, setOf(Vilkårsperiode(periode, Utfall.OPPFYLT, false, null, null)))
        val sykdomsVilkåret =
            Vilkår(Vilkårtype.SYKDOMSVILKÅRET, setOf(Vilkårsperiode(periode, Utfall.OPPFYLT, false, null, null)))
        val bistandVilkåret =
            Vilkår(Vilkårtype.BISTANDSVILKÅRET, setOf(Vilkårsperiode(periode, Utfall.OPPFYLT, false, null, null)))
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