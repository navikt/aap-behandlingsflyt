package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.dbstuff.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbstuff.transaction
import no.nav.aap.behandlingsflyt.mars
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.SakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonopplysningRepositoryTest {

    private companion object {
        private val dataSource = InitTestDatabase.dataSource

        private val ident = Ident("123123123124")
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private lateinit var sak: Sak

    @BeforeEach
    fun setup() {
        dataSource.transaction { connection ->
            sak = SakRepository(connection)
                .finnEllerOpprett(PersonRepository(connection).finnEllerOpprett(ident), periode)
        }
    }

    @AfterEach
    fun putes() {
        dataSource.transaction { connection ->
            connection.execute("TRUNCATE TABLE SAK CASCADE")
        }
    }

    @Test
    fun `Finner ikke personopplysninger hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val personopplysningRepository = PersonopplysningRepository(connection)
            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling.id)
            assertThat(personopplysningGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter personopplysninger`() {
        dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val personopplysningRepository = PersonopplysningRepository(connection)
            personopplysningRepository.lagre(behandling.id, Personopplysning(Fødselsdato(17 mars 1992)))
            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling.id)
            assertThat(personopplysningGrunnlag?.personopplysning).isEqualTo(Personopplysning(Fødselsdato(17 mars 1992)))
        }
    }

    @Test
    fun `Kopierer personopplysninger fra en behandling til en annen`() {
        dataSource.transaction { connection ->
            val behandling1 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")
            val behandling2 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val personopplysningRepository = PersonopplysningRepository(connection)
            personopplysningRepository.lagre(behandling1.id, Personopplysning(Fødselsdato(17 mars 1992)))
            personopplysningRepository.kopier(
                fraBehandling = behandling1.id,
                tilBehandling = behandling2.id
            )
            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling2.id)
            assertThat(personopplysningGrunnlag?.personopplysning).isEqualTo(Personopplysning(Fødselsdato(17 mars 1992)))
        }
    }
}
