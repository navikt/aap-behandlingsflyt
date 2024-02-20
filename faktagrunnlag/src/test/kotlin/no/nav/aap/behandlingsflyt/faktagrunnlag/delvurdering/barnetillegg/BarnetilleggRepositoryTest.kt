package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne.FakePdlGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakOgBehandlingService
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BarnetilleggRepositoryTest {

    @Test
    fun `Finner ikke barnetilleggGrunnlag hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val barnetilleggRepository = BarnetilleggRepository(connection)
            val barnetilleggGrunnlag = barnetilleggRepository.hentHvisEksisterer(behandling.id)
            assertThat(barnetilleggGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter barnetilleggGrunnlag`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val barnetilleggRepository = BarnetilleggRepository(connection)
            val barnetilleggPeriode = listOf(
                BarnetilleggPeriode(
                    Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                    setOf(Ident("12345678910"), Ident("12345678911"))
                ),
                BarnetilleggPeriode(
                    Periode(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 2)),
                    setOf(Ident("12345678910"))
                )
            )

            barnetilleggRepository.lagre(behandling.id, barnetilleggPeriode)

            val barnetilleggGrunnlag = barnetilleggRepository.hentHvisEksisterer(
                behandling.id
            )

            assertThat(barnetilleggGrunnlag?.perioder).isEqualTo(
                barnetilleggPeriode
            )
        }
    }

    @Test
    fun `lager nytt deaktiverer og lager nytt grunnlag ved ny lagring`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val barnetilleggRepository = BarnetilleggRepository(connection)
            val barnetilleggPeriode1 = BarnetilleggPeriode(
                Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                setOf(Ident("12345678910"), Ident("12345678911"))
            )


            val barnetilleggPeriode2 = BarnetilleggPeriode(
                Periode(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 2)),
                setOf(Ident("12345678910"))
            )


            barnetilleggRepository.lagre(behandling.id, listOf(barnetilleggPeriode1))
            barnetilleggRepository.lagre(behandling.id, listOf(barnetilleggPeriode2))

            val barnetilleggGrunnlag = barnetilleggRepository.hentHvisEksisterer(
                behandling.id
            )

            assertThat(barnetilleggGrunnlag?.perioder).isEqualTo(
                listOf(barnetilleggPeriode2)
            )
        }
    }

    @Test
    fun `Kopierer barnetilleggGrunnlag fra en behandling til en annen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val barnetilleggRepository = BarnetilleggRepository(connection)
            barnetilleggRepository.lagre(
                behandling1.id,
                listOf(
                    BarnetilleggPeriode(
                        Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                        setOf(Ident("12345678910"), Ident("12345678911"))
                    ),
                )
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = behandling(connection, sak)
            barnetilleggRepository.kopier(behandling1.id, behandling2.id)

            val barnetilleggGrunnlag = barnetilleggRepository.hentHvisEksisterer(behandling2.id)
            assertThat(barnetilleggGrunnlag?.perioder)
                .containsExactly(
                    BarnetilleggPeriode(
                        Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                        setOf(Ident("12345678910"), Ident("12345678911"))
                    ),
                )
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(
            ident(),
            periode
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(sak.saksnummer).behandling
    }
}