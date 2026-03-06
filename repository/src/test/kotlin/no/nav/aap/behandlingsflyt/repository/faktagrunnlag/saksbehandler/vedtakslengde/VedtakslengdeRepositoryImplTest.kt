package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.vedtakslengde

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeSluttdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.*
import java.time.Instant
import java.time.LocalDate

class VedtakslengdeRepositoryImplTest {
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
        RecursiveComparisonConfiguration.builder().withIgnoredFields("opprettet", "id").build()

    private fun vurdering(behandlingId: BehandlingId) = VedtakslengdeVurdering(
        sluttdato = LocalDate.of(2023, 12, 31),
        utvidetMed = ÅrMedHverdager.FØRSTE_ÅR,
        vurdertAv = Bruker("Z654321"),
        vurdertIBehandling = behandlingId,
        opprettet = Instant.parse("2023-01-01T12:00:00Z"),
        sluttdatoÅrsak = listOf(VedtakslengdeSluttdatoÅrsak.ETT_ÅR_VARIGHET)
    )

    @Test
    fun `lagrer og henter vurdering`() {
        val sak = dataSource.transaction { sak(it, Periode(1 januar 2022, 31.desember(2023))) }
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }
        val vurdering = vurdering(behandling.id)

        dataSource.transaction {
            VedtakslengdeRepositoryImpl(it).lagre(behandling.id, listOf(vurdering))
        }

        val hentet = dataSource.transaction {
            VedtakslengdeRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(hentet).usingRecursiveComparison(sammenligner).isEqualTo(
            VedtakslengdeGrunnlag(listOf(vurdering))
        )
    }

    @Test
    fun `slett fjerner vurdering`() {
        val sak = dataSource.transaction { sak(it, Periode(1 januar 2022, 31.desember(2023))) }
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }
        val vurdering = vurdering(behandling.id)

        dataSource.transaction {
            VedtakslengdeRepositoryImpl(it).lagre(behandling.id, listOf(vurdering))
        }
        dataSource.transaction {
            VedtakslengdeRepositoryImpl(it).slett(behandling.id)
        }
        val hentetEtterSletting = dataSource.transaction {
            VedtakslengdeRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(hentetEtterSletting).isNull()
    }

    @Test
    fun `kopierer vurdering til ny behandling`() {
        val sak = dataSource.transaction { sak(it, Periode(1 januar 2022, 31.desember(2023))) }
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }
        val vurdering = vurdering(behandling.id)

        dataSource.transaction {
            VedtakslengdeRepositoryImpl(it).lagre(behandling.id, listOf(vurdering))
        }

        dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)
            val behandling2 = finnEllerOpprettBehandling(connection, sak)
            val grunnlag2 = VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(behandling2.id)
            assertThat(grunnlag2)
                .usingRecursiveComparison(sammenligner)
                .isEqualTo(VedtakslengdeGrunnlag(listOf(vurdering)))
        }
    }

    @Test
    fun `lagrer og henter flere vurderinger med ulike sluttdatoer`() {
        val sak = dataSource.transaction { sak(it, Periode(1 januar 2022, 31.desember(2023))) }
        val behandling1 = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }

        val vurdering1 = VedtakslengdeVurdering(
            sluttdato = LocalDate.of(2023, 6, 30),
            utvidetMed = ÅrMedHverdager.FØRSTE_ÅR,
            vurdertAv = Bruker("Z654321"),
            vurdertIBehandling = behandling1.id,
            opprettet = Instant.parse("2023-01-01T12:00:00Z"),
            sluttdatoÅrsak = listOf(VedtakslengdeSluttdatoÅrsak.ETT_ÅR_VARIGHET)
        )

        dataSource.transaction {
            VedtakslengdeRepositoryImpl(it).lagre(behandling1.id, listOf(vurdering1))
            BehandlingRepositoryImpl(it).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
        }

        val behandling2 = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }

        val vurdering2 = VedtakslengdeVurdering(
            sluttdato = LocalDate.of(2024, 6, 30),
            utvidetMed = ÅrMedHverdager.ANDRE_ÅR,
            vurdertAv = Bruker("Z654321"),
            vurdertIBehandling = behandling2.id,
            opprettet = Instant.parse("2024-01-01T12:00:00Z"),
            sluttdatoÅrsak = listOf(VedtakslengdeSluttdatoÅrsak.ETT_ÅR_VARIGHET)
        )

        dataSource.transaction {
            val vedtakslengdeRepository = VedtakslengdeRepositoryImpl(it)
            val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(behandling1.id)
            val tidligereVurderinger = vedtakslengdeGrunnlag?.vurderinger ?: emptyList()
            vedtakslengdeRepository.lagre(behandling2.id, tidligereVurderinger + listOf(vurdering2))
        }

        val hentet = dataSource.transaction {
            VedtakslengdeRepositoryImpl(it).hentHvisEksisterer(behandling2.id)
        }

        assertThat(hentet).isNotNull
        assertThat(hentet!!.vurderinger).hasSize(2)
        assertThat(hentet.vurderinger.map { it.sluttdato }).containsExactly(
            LocalDate.of(2023, 6, 30),
            LocalDate.of(2024, 6, 30),
        )
        assertThat(hentet.vurderinger.map { it.vurdertIBehandling }).containsExactly(
            behandling1.id,
            behandling2.id,
        )
        assertThat(hentet.gjeldendeVurdering()?.sluttdato).isEqualTo(LocalDate.of(2024, 6, 30))
    }
}