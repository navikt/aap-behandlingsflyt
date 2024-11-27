package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedUtlederService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.tomUnderveisInput
import no.nav.aap.behandlingsflyt.dbtestdata.MockDataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.PliktkortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.sql.DataSource

class UnderveisServiceTest {

    private val dataSource: DataSource = MockDataSource()
    private val kvoter = Kvoter.create(260*3, 260/2)

    @Test
    fun `skal vurdere alle reglene`() {
        dataSource.transaction { connection ->
            val underveisService =
                UnderveisService(
                    SakOgBehandlingService(connection),
                    VilkårsresultatRepository(connection),
                    PliktkortRepository(connection),
                    UnderveisRepository(connection),
                    AktivitetspliktRepository(connection),
                    EtAnnetStedUtlederService(
                        BarnetilleggRepository(connection),
                        InstitusjonsoppholdRepository(connection),
                        SakOgBehandlingService(connection)
                    ),
                    ArbeidsevneRepository(connection),
                    MeldepliktRepository(connection),
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
            val relevanteVilkår = listOf(aldersVilkåret, bistandVilkåret, medlemskapVilkåret, sykdomsVilkåret)
            val input = tomUnderveisInput.copy(
                rettighetsperiode = periode,
                relevanteVilkår = relevanteVilkår,
                opptrappingPerioder = listOf(Periode(søknadsdato.plusYears(2), søknadsdato.plusYears(3))),
                kvoter = kvoter
            )

            val vurderingTidslinje = underveisService.vurderRegler(input)

            assertThat(vurderingTidslinje).isNotEmpty()
        }
    }
}
