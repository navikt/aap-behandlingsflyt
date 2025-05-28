package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InstitusjonsoppholdRepositoryImplTest {
    @Test
    fun `Tom tidslinje dersom ingen opphold finnes`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val institusjonsoppholdRepository = InstitusjonsoppholdRepositoryImpl(connection)
            val institusjonsoppholdTidslinje = institusjonsoppholdRepository.hentHvisEksisterer(behandling.id)
            assertThat(institusjonsoppholdTidslinje).isNull()
        }
    }

    @Test
    fun `kan lagre og hente fra raw data fra gateway`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val institusjonsoppholdRepository = InstitusjonsoppholdRepositoryImpl(connection)
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


            val institusjonsoppholdTidslinje =
                requireNotNull(institusjonsoppholdRepository.hent(behandling.id).oppholdene?.opphold)
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
        InitTestDatabase.freshDatabase().transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val institusjonsoppholdRepository = InstitusjonsoppholdRepositoryImpl(connection)
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
            val sak2 = sak(connection)
            val behandling2 = finnEllerOpprettBehandling(connection, sak2)

            institusjonsoppholdRepository.kopier(behandling.id, behandling2.id)

            val institusjonsoppholdTidslinje2 =
                requireNotNull(institusjonsoppholdRepository.hent(behandling2.id).oppholdene?.opphold)
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
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection),
        ).finnEllerOpprett(ident(), periode)
    }
}