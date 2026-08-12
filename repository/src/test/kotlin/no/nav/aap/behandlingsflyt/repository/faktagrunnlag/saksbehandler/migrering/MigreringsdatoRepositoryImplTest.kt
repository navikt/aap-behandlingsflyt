package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.migrering

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.migrering.MigreringsdatoGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.migrering.MigreringsdatoVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MigreringsdatoRepositoryImplTest {

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

    private val sammenligner: RecursiveComparisonConfiguration =
        RecursiveComparisonConfiguration.builder().withIgnoredFields("opprettet").build()

    private fun vurdering(behandlingId: BehandlingId) = MigreringsdatoVurdering(
        migreringsdato = LocalDate.of(2024, 1, 15),
        vurdertAv = Bruker("Z654321"),
        vurdertIBehandling = behandlingId,
        opprettet = LocalDateTime.of(2024, 1, 15, 10, 0, 0),
    )

    @Test
    fun `hentHvisEksisterer returnerer null når ingen vurdering eksisterer`() {
        val sak = dataSource.transaction { sak(it, 1 januar 2022) }
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }

        val hentet = dataSource.transaction {
            MigreringsdatoRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(hentet).isNull()
    }

    @Test
    fun `lagrer og henter vurdering`() {
        val sak = dataSource.transaction { sak(it, 1 januar 2022) }
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }
        val vurdering = vurdering(behandling.id)

        dataSource.transaction {
            MigreringsdatoRepositoryImpl(it).lagreVurdering(behandling.id, vurdering)
        }

        val hentet = dataSource.transaction {
            MigreringsdatoRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(hentet).usingRecursiveComparison(sammenligner).isEqualTo(
            MigreringsdatoGrunnlag(listOf(vurdering))
        )
    }

    @Test
    fun `lagre ny vurdering deaktiverer gammelt grunnlag og oppretter nytt med bare den nye vurderingen`() {
        val sak = dataSource.transaction { sak(it, 1 januar 2022) }
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }

        val førsteVurdering = vurdering(behandling.id)
        val andreVurdering = MigreringsdatoVurdering(
            migreringsdato = LocalDate.of(2024, 6, 1),
            vurdertAv = Bruker("Z654321"),
            vurdertIBehandling = behandling.id,
            opprettet = LocalDateTime.of(2024, 6, 1, 12, 0, 0),
        )

        dataSource.transaction { MigreringsdatoRepositoryImpl(it).lagreVurdering(behandling.id, førsteVurdering) }
        dataSource.transaction { MigreringsdatoRepositoryImpl(it).lagreVurdering(behandling.id, andreVurdering) }

        val hentet = dataSource.transaction {
            MigreringsdatoRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(hentet).isNotNull
        assertThat(hentet!!.vurderinger).hasSize(1)
        assertThat(hentet.vurderinger.single().migreringsdato).isEqualTo(LocalDate.of(2024, 6, 1))
    }

    @Test
    fun `kopier peker nytt grunnlag til samme vurderinger uten å duplisere vurdering-rader`() {
        val sak = dataSource.transaction { sak(it, 1 januar 2022) }
        val behandling1 = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }
        val vurdering = vurdering(behandling1.id)

        dataSource.transaction { MigreringsdatoRepositoryImpl(it).lagreVurdering(behandling1.id, vurdering) }

        val behandling2 = dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
            finnEllerOpprettBehandling(connection, sak)
        }

        dataSource.transaction {
            MigreringsdatoRepositoryImpl(it).kopier(behandling1.id, behandling2.id)
        }

        val hentetBehandling1 = dataSource.transaction {
            MigreringsdatoRepositoryImpl(it).hentHvisEksisterer(behandling1.id)
        }
        val hentetBehandling2 = dataSource.transaction {
            MigreringsdatoRepositoryImpl(it).hentHvisEksisterer(behandling2.id)
        }

        assertThat(hentetBehandling2).usingRecursiveComparison(sammenligner).isEqualTo(hentetBehandling1)
        assertThat(hentetBehandling2!!.vurderinger.single().migreringsdato).isEqualTo(LocalDate.of(2024, 1, 15))
    }

    @Test
    fun `etter kopier kan ny behandling lagre sin egen vurdering uten å påvirke original`() {
        val sak = dataSource.transaction { sak(it, 1 januar 2022) }
        val behandling1 = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }
        val opprinneligVurdering = vurdering(behandling1.id)

        dataSource.transaction { MigreringsdatoRepositoryImpl(it).lagreVurdering(behandling1.id, opprinneligVurdering) }

        val behandling2 = dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
            finnEllerOpprettBehandling(connection, sak)
        }

        dataSource.transaction { MigreringsdatoRepositoryImpl(it).kopier(behandling1.id, behandling2.id) }

        val nyVurdering = MigreringsdatoVurdering(
            migreringsdato = LocalDate.of(2025, 3, 1),
            vurdertAv = Bruker("Z654321"),
            vurdertIBehandling = behandling2.id,
            opprettet = LocalDateTime.of(2025, 3, 1, 9, 0, 0),
        )
        dataSource.transaction { MigreringsdatoRepositoryImpl(it).lagreVurdering(behandling2.id, nyVurdering) }

        val hentetBehandling1 = dataSource.transaction {
            MigreringsdatoRepositoryImpl(it).hentHvisEksisterer(behandling1.id)
        }
        val hentetBehandling2 = dataSource.transaction {
            MigreringsdatoRepositoryImpl(it).hentHvisEksisterer(behandling2.id)
        }

        assertThat(hentetBehandling1!!.vurderinger.single().migreringsdato).isEqualTo(LocalDate.of(2024, 1, 15))
        assertThat(hentetBehandling2!!.vurderinger.single().migreringsdato).isEqualTo(LocalDate.of(2025, 3, 1))
    }

    @Test
    fun `slett fjerner alle data`() {
        val sak = dataSource.transaction { sak(it, 1 januar 2022) }
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }

        dataSource.transaction { MigreringsdatoRepositoryImpl(it).lagreVurdering(behandling.id, vurdering(behandling.id)) }
        dataSource.transaction { MigreringsdatoRepositoryImpl(it).slett(behandling.id) }

        val hentet = dataSource.transaction {
            MigreringsdatoRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(hentet).isNull()
    }
}
