package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingVurdering
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.avbrytrevurdering.AvbrytRevurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.overganguføre.OvergangUføreRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

internal class OvergangUføreRepositoryImplTest {
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

    @Test
    fun `Finner ikke overgang til uføre grunnlag hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val overgangUføreRepository = OvergangUføreRepositoryImpl(connection)
            val overgangUføreGrunnlag = overgangUføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(overgangUføreGrunnlag).isNull()
        }
    }

    @Test
    fun `lagrer og henter overgang uføre vurdering`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val overgangUføreRepository = OvergangUføreRepositoryImpl(connection)

            val testDate = LocalDate.of(2025, 1, 1)
            val expected = OvergangUføreVurdering(
                begrunnelse = "test",
                brukerHarSøktOmUføretrygd = true,
                brukerHarFåttVedtakOmUføretrygd = "NEI",
                brukerRettPåAAP = true,
                fom = testDate,
                vurdertAv = "Saks behandler",
                vurdertIBehandling = behandling.id
            )

            overgangUføreRepository.lagre(behandling.id, listOf(expected))
            val actual = overgangUføreRepository.hentHvisEksisterer(behandling.id)

            assertThat(actual?.vurderinger)
                .usingRecursiveComparison()
                .ignoringFields("opprettet")
                .isEqualTo(listOf(expected))
        }
    }

    @Test
    fun `test sletting`() {
        TestDataSource().use { dataSource ->
            dataSource.transaction { connection ->
                val sak = sak(connection)
                val behandling = finnEllerOpprettBehandling(connection, sak)
                val overgangUføreRepository = OvergangUføreRepositoryImpl(connection)
                overgangUføreRepository.lagre(
                    behandling.id,
                    listOf(
                        OvergangUføreVurdering(
                            begrunnelse = "test",
                            brukerHarSøktOmUføretrygd = true,
                            brukerHarFåttVedtakOmUføretrygd = "NEI",
                            brukerRettPåAAP = true,
                            fom = LocalDate.now(),
                            vurdertAv = "Saks behandler",
                            vurdertIBehandling = behandling.id

                        )
                    )
                )
                overgangUføreRepository.lagre(
                    behandling.id,
                    listOf(
                        OvergangUføreVurdering(
                            begrunnelse = "test",
                            brukerHarSøktOmUføretrygd = true,
                            brukerHarFåttVedtakOmUføretrygd = "NEI",
                            brukerRettPåAAP = true,
                            fom = LocalDate.now(),
                            vurdertAv = "Saks behandler",
                            vurdertIBehandling = behandling.id
                        )
                    )
                )
                assertDoesNotThrow { overgangUføreRepository.slett(behandling.id) }
            }
        }
    }

    @Test
    fun `historikk viser kun vurderinger fra tidligere behandlinger og ikke inkluderer vurdering fra avbrutt revurdering`() {
        val overgangUføreVurdering1 = { vurdertIBehandling: BehandlingId ->
            OvergangUføreVurdering(
                begrunnelse = "B1",
                brukerHarSøktOmUføretrygd = true,
                brukerHarFåttVedtakOmUføretrygd = "NEI",
                brukerRettPåAAP = true,
                fom = LocalDate.of(2024, 5, 22),
                vurdertAv = "Z00000",
            )
        }
        val overgangUføreVurdering2 = { vurdertIBehandling: BehandlingId ->
            OvergangUføreVurdering(
                begrunnelse = "B2",
                brukerHarSøktOmUføretrygd = true,
                brukerHarFåttVedtakOmUføretrygd = "JA",
                brukerRettPåAAP = true,
                fom = LocalDate.of(2024, 5, 1),
                vurdertAv = "Z00001",
            )
        }
        val overgangUføreVurdering3 = { vurdertIBehandling: BehandlingId ->
            OvergangUføreVurdering(
                begrunnelse = "B3",
                brukerHarSøktOmUføretrygd = true,
                brukerHarFåttVedtakOmUføretrygd = "NEI",
                brukerRettPåAAP = true,
                fom = LocalDate.of(2024, 4, 15),
                vurdertAv = "Z00002",
                vurdertIBehandling = vurdertIBehandling
            )
        }

        val førstegangsbehandling = dataSource.transaction { connection ->
            val overgangUføreRepo = OvergangUføreRepositoryImpl(connection)
            val sak = sak(connection)
            val førstegangsbehandling = finnEllerOpprettBehandling(connection, sak)

            overgangUføreRepo.lagre(førstegangsbehandling.id, listOf(overgangUføreVurdering1(førstegangsbehandling.id)))
            førstegangsbehandling
        }

        dataSource.transaction { connection ->
            val overgangUføreRepo = OvergangUføreRepositoryImpl(connection)
            val avbrytRevurderingRepo = AvbrytRevurderingRepositoryImpl(connection)
            val revurderingAvbrutt = revurderingOvergangUføre(connection, førstegangsbehandling)

            // Marker revurderingen som avbrutt
            avbrytRevurderingRepo.lagre(
                revurderingAvbrutt.id, AvbrytRevurderingVurdering(
                    AvbrytRevurderingÅrsak.REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL, "avbryte pga. feil",
                    Bruker("Z00000")
                )
            )
            overgangUføreRepo.lagre(revurderingAvbrutt.id, listOf(overgangUføreVurdering2(revurderingAvbrutt.id)))
        }

        dataSource.transaction { connection ->
            val overgangUføreRepo = OvergangUføreRepositoryImpl(connection)
            val revurdering = revurderingOvergangUføre(connection, førstegangsbehandling)

            overgangUføreRepo.lagre(revurdering.id, listOf(overgangUføreVurdering3(revurdering.id)))

            val historikk = overgangUføreRepo.hentHistoriskeOvergangUforeVurderinger(revurdering.sakId, revurdering.id)
            assertThat(historikk)
                .usingRecursiveComparison()
                .ignoringFields("opprettet")
                .isEqualTo(listOf(overgangUføreVurdering1(førstegangsbehandling.id)))
        }
    }

    private fun revurderingOvergangUføre(connection: DBConnection, behandling: Behandling): Behandling {
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            behandling.sakId,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = behandling.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.OVERGANG_UFORE)),
                årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            )
        )
    }

}