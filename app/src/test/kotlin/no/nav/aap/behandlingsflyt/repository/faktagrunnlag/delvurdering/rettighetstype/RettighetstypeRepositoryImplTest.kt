package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.rettighetstype

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeFaktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeGrunnlag
import no.nav.aap.behandlingsflyt.help.genererVilkårsresultat
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

class RettighetstypeRepositoryImplTest {

    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setUp() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    @Test
    fun `lagre, hente og slette rettighetstypegrunnlag`() {
        val rettighetsperiode = Periode(1 januar 2020, Tid.MAKS)
        val sak = dataSource.transaction { sak(it, rettighetsperiode) }
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }

        val periodeMedSpe = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusDays(10))
        val periodeMedBistandsbehov = Periode(periodeMedSpe.tom.plusDays(1), periodeMedSpe.tom.plusDays(1).plusYears(3))
        val rettighetstypeTidslinje = tidslinjeOf(
            periodeMedSpe to RettighetsType.SYKEPENGEERSTATNING,
            periodeMedBistandsbehov to RettighetsType.BISTANDSBEHOV
        )

        val input = RettighetstypeFaktagrunnlag(
            vilkårsresultat = genererVilkårsresultat(rettighetsperiode),
        )

        // Lagre
        dataSource.transaction {
            RettighetstypeRepositoryImpl(it).lagre(behandling.id, rettighetstypeTidslinje, input, versjon = "1")
        }

        // Hent rettighetstype
        val uthentet = dataSource.transaction {
            RettighetstypeRepositoryImpl(it).hent(behandling.id)
        }
        assertThat(uthentet.rettighetstypeTidslinje.segmenter()).hasSize(2)

        assertThat(uthentet)
            .isEqualTo(RettighetstypeGrunnlag(rettighetstypeTidslinje))

        // Hent sporing
        val sporing = dataSource.transaction {
            RettighetstypeRepositoryImpl(it).hentSporingHvisEksisterer(behandling.id)
        }

        assertThat(sporing).isNotNull()
        assertThat(sporing!!.first).isEqualTo("1")
        assertThat(sporing.second).isEqualTo("{\"vilkårsresultat\":{\"id\$behandlingsflyt_behandlingsflyt\":null}}")

        // Slett
        dataSource.transaction {
            val antallRaderSlettet = RettighetstypeRepositoryImpl(it).slettMedCount(behandling.id)
            assertThat(antallRaderSlettet).isEqualTo(1 + 1 + 2 + 1) // grunnlag + perioder + periode + faktagrunnlag
        }

        val slettetGrunnlag = dataSource.transaction {
            RettighetstypeRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        val slettetSporing = dataSource.transaction {
            RettighetstypeRepositoryImpl(it).hentSporingHvisEksisterer(behandling.id)
        }
        assertThat(slettetGrunnlag).isNull()
        assertThat(slettetSporing).isNull()
    }

    @Test
    fun Kopier() {
        val rettighetsperiode = Periode(1 januar 2020, Tid.MAKS)
        val sak = dataSource.transaction { sak(it, rettighetsperiode) }
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }

        val periodeMedRett = Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusYears(3))
        val rettighetstypeTidslinje = tidslinjeOf(
            periodeMedRett to RettighetsType.BISTANDSBEHOV
        )

        val input = RettighetstypeFaktagrunnlag(
            vilkårsresultat = genererVilkårsresultat(rettighetsperiode),
        )

        // Lagre
        dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)


            RettighetstypeRepositoryImpl(connection).lagre(
                behandling.id, rettighetstypeTidslinje, input, versjon = "1"
            )

            // Kopier
            val nyBehandling = finnEllerOpprettBehandling(connection, sak)

            // Hent kopiert
            val kopiert = RettighetstypeRepositoryImpl(connection).hent(
                nyBehandling.id
            )
            assertThat(kopiert.rettighetstypeTidslinje.segmenter()).hasSize(1)

            assertThat(kopiert)
                .isEqualTo(RettighetstypeGrunnlag(rettighetstypeTidslinje))

        }
    }
}