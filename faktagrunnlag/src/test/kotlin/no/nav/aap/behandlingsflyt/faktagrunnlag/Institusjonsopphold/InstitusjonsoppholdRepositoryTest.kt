package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne.FakePdlGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.verdityper.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InstitusjonsoppholdRepositoryTest {
    @Test
    fun `Tom tidslinje dersom ingen opphold finnes`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)
            val institusjonsoppholdTidslinje = institusjonsoppholdRepository.hentHvisEksisterer(behandling.id)
            assertThat(institusjonsoppholdTidslinje).isNull()
        }
    }

    @Test
    fun `kan lagre og hente fra raw data fra gateway`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)
            val institusjonsopphold = listOf(
                Institusjonsopphold.nyttOpphold(
                    "AS",
                    "A",
                    LocalDate.now(),
                    LocalDate.now().plusDays(1),
                    "123456789",
                    "Azkaban"
                )
            )
            institusjonsoppholdRepository.lagreOpphold(behandling.id, institusjonsopphold)


            val institusjonsoppholdTidslinje = institusjonsoppholdRepository.hent(behandling.id)
            assertThat(institusjonsoppholdTidslinje).hasSize(1)
            assertThat(institusjonsoppholdTidslinje.first().verdi).isEqualTo(
                Institusjon(
                    Institusjonstype.AS,
                    Oppholdstype.A,
                    "123456789",
                    "Azkaban"
                )
            )
        }
    }


    @Test
    fun kopier() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)
            val institusjonsopphold = listOf(
                Institusjonsopphold.nyttOpphold("AS", "A", LocalDate.now(), LocalDate.now().plusDays(1), "123456789", "Azkaban")
            )
            institusjonsoppholdRepository.lagreOpphold(behandling.id, institusjonsopphold)
            val sak2 = sak(connection)
            val behandling2 = behandling(connection, sak2)

            institusjonsoppholdRepository.kopier(behandling.id, behandling2.id)

            val institusjonsoppholdTidslinje2 = institusjonsoppholdRepository.hent(behandling2.id)
            assertThat(institusjonsoppholdTidslinje2).hasSize(1)
            assertThat(institusjonsoppholdTidslinje2.first().verdi).isEqualTo(
                Institusjon(
                    Institusjonstype.AS,
                    Oppholdstype.A,
                    "123456789",
                    "Azkaban"
                )
            )

        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(connection, FakePdlGateway).finnEllerOpprett(ident(), periode)
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(
            sak.saksnummer,
            listOf(Årsak(EndringType.MOTTATT_SØKNAD))
        ).behandling
    }
}