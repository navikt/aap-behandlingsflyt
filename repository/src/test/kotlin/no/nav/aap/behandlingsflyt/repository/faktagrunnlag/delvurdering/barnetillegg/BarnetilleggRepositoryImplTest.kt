package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.barnetillegg

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.barnetillegg.BarnetilleggRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BarnetilleggRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    @Test
    fun `Finner ikke barnetilleggGrunnlag hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val barnetilleggRepository = BarnetilleggRepositoryImpl(connection)
            val barnetilleggGrunnlag = barnetilleggRepository.hentHvisEksisterer(behandling.id)
            assertThat(barnetilleggGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter barnetilleggGrunnlag`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val barnetilleggRepository = BarnetilleggRepositoryImpl(connection)
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
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val barnetilleggRepository = BarnetilleggRepositoryImpl(connection)
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
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val barnetilleggRepository = BarnetilleggRepositoryImpl(connection)
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
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

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

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection),
            BehandlingRepositoryImpl(connection),
            TrukketSøknadService(
                AvklaringsbehovRepositoryImpl(connection),
                TrukketSøknadRepositoryImpl(connection)
            ),
        ).finnEllerOpprett(
            ident(),
            periode
        )
    }
}