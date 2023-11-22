package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.mars
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.SakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class PersonopplysningRepositoryTest {

    @Test
    fun `Finner ikke personopplysninger hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = behandling(connection, sak(connection))

            val personopplysningRepository = PersonopplysningRepository(connection)
            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling.id)
            assertThat(personopplysningGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter personopplysninger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = behandling(connection, sak(connection))

            val personopplysningRepository = PersonopplysningRepository(connection)
            personopplysningRepository.lagre(behandling.id, Personopplysning(Fødselsdato(17 mars 1992)))
            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling.id)
            assertThat(personopplysningGrunnlag?.personopplysning).isEqualTo(Personopplysning(Fødselsdato(17 mars 1992)))
        }
    }

    @Test
    fun `Lagrer ikke like opplysninger flere ganger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val personopplysningRepository = PersonopplysningRepository(connection)
            personopplysningRepository.lagre(behandling.id, Personopplysning(Fødselsdato(17 mars 1992)))
            personopplysningRepository.lagre(behandling.id, Personopplysning(Fødselsdato(18 mars 1992)))
            personopplysningRepository.lagre(behandling.id, Personopplysning(Fødselsdato(18 mars 1992)))

            val opplysninger =
                connection.queryList(
                    """
                    SELECT FODSELSDATO
                    FROM BEHANDLING b
                    INNER JOIN PERSONOPPLYSNING p ON b.ID = p.BEHANDLING_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row -> row.getLocalDate("FODSELSDATO") }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(17 mars 1992, 18 mars 1992)
        }
    }

    @Test
    fun `Kopierer personopplysninger fra en behandling til en annen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val personopplysningRepository = PersonopplysningRepository(connection)
            personopplysningRepository.lagre(behandling1.id, Personopplysning(Fødselsdato(17 mars 1992)))
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")
            val behandling2 = behandling(connection, sak)

            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling2.id)
            assertThat(personopplysningGrunnlag?.personopplysning).isEqualTo(Personopplysning(Fødselsdato(17 mars 1992)))
        }
    }

    @Test
    fun `Kopierer personopplysninger fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val personopplysningRepository = PersonopplysningRepository(connection)
            personopplysningRepository.lagre(behandling1.id, Personopplysning(Fødselsdato(16 mars 1992)))
            personopplysningRepository.lagre(behandling1.id, Personopplysning(Fødselsdato(17 mars 1992)))
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")

            val behandling2 = behandling(connection, sak)

            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling2.id)
            assertThat(personopplysningGrunnlag?.personopplysning).isEqualTo(Personopplysning(Fødselsdato(17 mars 1992)))
        }
    }

    @Test
    fun `Lagrer nye opplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)
            val personopplysningRepository = PersonopplysningRepository(connection)

            personopplysningRepository.lagre(behandling.id, Personopplysning(Fødselsdato(17 mars 1992)))
            val orginaltGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.personopplysning).isEqualTo(Personopplysning(Fødselsdato(17 mars 1992)))

            personopplysningRepository.lagre(behandling.id, Personopplysning(Fødselsdato(18 mars 1992)))
            val oppdatertGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.personopplysning).isEqualTo(Personopplysning(Fødselsdato(18 mars 1992)))

            data class Opplysning(val behandlingId: Long, val fødselsdato: LocalDate, val aktiv: Boolean)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT BEHANDLING_ID, FODSELSDATO, AKTIV
                    FROM BEHANDLING b
                    INNER JOIN PERSONOPPLYSNING p ON b.ID = p.BEHANDLING_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Opplysning(
                            behandlingId = row.getLong("BEHANDLING_ID"),
                            fødselsdato = row.getLocalDate("FODSELSDATO"),
                            aktiv = row.getBoolean("AKTIV")
                        )
                    }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(
                    Opplysning(behandling.id.toLong(), 17 mars 1992, false),
                    Opplysning(behandling.id.toLong(), 18 mars 1992, true)
                )
        }
    }

    private companion object {
        private val identTeller = AtomicInteger(0)
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        private fun ident(): Ident {
            return Ident(identTeller.getAndAdd(1).toString())
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return SakRepository(connection).finnEllerOpprett(
            person = PersonRepository(connection).finnEllerOpprett(ident()),
            periode = periode
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        val behandling = BehandlingRepository(connection).finnSisteBehandlingFor(sak.id)
        if (behandling == null || behandling.status().erAvsluttet()) {
            return BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
        }
        return behandling
    }
}
