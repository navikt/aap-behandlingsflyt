package no.nav.aap.behandlingsflyt.faktagrunnlag.barn

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.ManueltBarnVurdeirng
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.ManuellebarnVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ManuellebarnVurderingRepositoryTest {
    @Test
    fun `Finner ikke barnetilleggGrunnlag hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val barnetilleggRepository = ManuellebarnVurderingRepository(connection)
            val barnetilleggGrunnlag = barnetilleggRepository.hentHvisEksisterer(behandling.id)
            Assertions.assertThat(barnetilleggGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter barnetilleggGrunnlag`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val barneVurderingRepository = ManuellebarnVurderingRepository(connection)
            val barneVurderingPeriode = setOf(
                ManueltBarnVurdeirng(
                    Ident("12345678910"),
                    "Beskrivelse",
                    true,
                    listOf(Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)))
                ),
                ManueltBarnVurdeirng(
                    Ident("12345678910"),
                    "Enda en beskrivelse",
                    false,
                    listOf(Periode(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 2)))
                )
            )

            barneVurderingRepository.lagre(behandling.id, barneVurderingPeriode)

            val barnVurderingGrunnlag = barneVurderingRepository.hentHvisEksisterer(
                behandling.id
            )

            Assertions.assertThat(barnVurderingGrunnlag?.vurdering?.barn).isEqualTo(
                barneVurderingPeriode
            )
        }
    }

    @Test
    fun `lager nytt deaktiverer og lager nytt grunnlag ved ny lagring`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val barnVurderingRepository = ManuellebarnVurderingRepository(connection)
            val barneVurderingPeriode1 = ManueltBarnVurdeirng(
                Ident("12345678910"),
                "Beskrivelse",
                false,
                listOf(Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)))
            )

            val barneVurderingPeriode2 = ManueltBarnVurdeirng(
                Ident("12345678910"),
                "Beskrivelse",
                false,
                listOf(Periode(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 2)))
            )


            barnVurderingRepository.lagre(behandling.id, setOf(barneVurderingPeriode1))
            barnVurderingRepository.lagre(behandling.id, setOf(barneVurderingPeriode2))

            val barnetilleggGrunnlag = barnVurderingRepository.hentHvisEksisterer(
                behandling.id
            )

            Assertions.assertThat(barnetilleggGrunnlag?.vurdering?.barn).isEqualTo(
                setOf(barneVurderingPeriode2)
            )
        }
    }

    @Test
    fun `Kopierer barnetilleggGrunnlag fra en behandling til en annen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val barneVurderingRepository = ManuellebarnVurderingRepository(connection)
            barneVurderingRepository.lagre(
                behandling1.id,
                setOf(
                    ManueltBarnVurdeirng(
                        Ident("12345678910"),
                        "Beskrivelse",
                        false,
                        listOf(Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)))
                    ),
                )
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = behandling(connection, sak)

            val barnetilleggGrunnlag = barneVurderingRepository.hentHvisEksisterer(behandling2.id)
            Assertions.assertThat(barnetilleggGrunnlag?.vurdering?.barn)
                .containsExactly(
                    ManueltBarnVurdeirng(
                        Ident("12345678910"),
                        "Beskrivelse",
                        false,
                        listOf(Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)))
                    )
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
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(EndringType.MOTTATT_SØKNAD))
        ).behandling
    }
}