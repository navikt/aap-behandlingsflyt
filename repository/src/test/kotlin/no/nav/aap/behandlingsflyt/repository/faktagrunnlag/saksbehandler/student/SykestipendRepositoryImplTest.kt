package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant

class SykestipendRepositoryImplTest {
    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    val sammenligner: RecursiveComparisonConfiguration =
        RecursiveComparisonConfiguration.builder().withIgnoredFields("opprettet", "id").build()

    @Test
    fun `lagre, hente ut, slette`() {
        val sak = dataSource.transaction { sak(it, Periode(1 januar 2022, 31.desember(2023))) }

        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }

        val vurdering = SykestipendVurdering(
            begrunnelse = "begrunnelse",
            perioder = setOf(
                Periode(
                    fom = 1 januar 2022,
                    tom = 31 desember 2022
                )
            ),
            vurdertAv = Bruker("Z123456"),
            vurdertIBehandling = behandling.id,
            opprettet = Instant.parse("2023-01-01T12:00:00Z")
        )

        dataSource.transaction {
            SykestipendRepositoryImpl(it).lagre(
                behandling.id,
                vurdering
            )
        }

        val hentet = dataSource.transaction {
            SykestipendRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(hentet).usingRecursiveComparison(sammenligner).isEqualTo(
            SykestipendGrunnlag(
                vurdering = vurdering
            )
        )
        
        dataSource.transaction {
            SykestipendRepositoryImpl(it).slett(behandling.id)
        }

        val hentetEtterSletting = dataSource.transaction {
            SykestipendRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(hentetEtterSletting).isNull()
    }

    @Test
    fun `Kopier sykestipend`() {
        val sak = dataSource.transaction { sak(it, Periode(1 januar 2022, 31.desember(2023))) }

        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak)
        }

        val vurdering = SykestipendVurdering(
            begrunnelse = "begrunnelse",
            perioder = setOf(
                Periode(
                    fom = 1 januar 2022,
                    tom = 31 desember 2022
                )
            ),
            vurdertAv = Bruker("Z123456"),
            vurdertIBehandling = behandling.id,
            opprettet = Instant.parse("2023-01-01T12:00:00Z")
        )

        dataSource.transaction {
            SykestipendRepositoryImpl(it).lagre(
                behandling.id,
                vurdering
            )
        }

        dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)

            // Kopier
            val behandling2 = finnEllerOpprettBehandling(connection, sak)
            val grunnlag2 = SykestipendRepositoryImpl(connection).hentHvisEksisterer(behandling2.id)
            assertThat(grunnlag2?.vurdering)
                .usingRecursiveComparison(sammenligner)
                .isEqualTo(vurdering)
        }
    }
}

