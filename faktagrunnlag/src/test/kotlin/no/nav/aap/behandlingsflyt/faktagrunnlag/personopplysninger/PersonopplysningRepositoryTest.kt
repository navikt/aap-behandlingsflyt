package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.april
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.dbtestdata.mars
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakOgBehandlingService
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class PersonopplysningRepositoryTest {

    @Test
    fun `Finner ikke personopplysninger hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val personopplysningRepository = PersonopplysningRepository(connection)
            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling.id)
            assertThat(personopplysningGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter personopplysninger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

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
                    SELECT p.FODSELSDATO
                    FROM BEHANDLING b
                    INNER JOIN PERSONOPPLYSNING_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN PERSONOPPLYSNING p ON g.PERSONOPPLYSNING_ID = p.ID
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
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = behandling(connection, sak)
            personopplysningRepository.kopier(behandling1.id, behandling2.id)

            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling2.id)
            assertThat(personopplysningGrunnlag?.personopplysning).isEqualTo(Personopplysning(Fødselsdato(17 mars 1992)))
        }
    }

    @Test
    fun `Kopiering av personopplysninger fra en behandling uten opplysningene skal ikke føre til feil`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val personopplysningRepository = PersonopplysningRepository(connection)
            assertDoesNotThrow {
                personopplysningRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
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
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }

            val behandling2 = behandling(connection, sak)
            personopplysningRepository.kopier(behandling1.id, behandling2.id)

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
                    SELECT b.ID, p.FODSELSDATO, g.AKTIV
                    FROM BEHANDLING b
                    INNER JOIN PERSONOPPLYSNING_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN PERSONOPPLYSNING p ON g.PERSONOPPLYSNING_ID = p.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Opplysning(
                            behandlingId = row.getLong("ID"),
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

    @Test
    fun `Ved kopiering av opplysninger fra en avsluttet behandling til en ny skal kun referansen kopieres, ikke hele raden`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val personopplysningRepository = PersonopplysningRepository(connection)
            personopplysningRepository.lagre(behandling1.id, Personopplysning(Fødselsdato(17 mars 1992)))
            personopplysningRepository.lagre(behandling1.id, Personopplysning(Fødselsdato(17 april 1992)))
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = behandling(connection, sak)
            personopplysningRepository.kopier(behandling1.id, behandling2.id)

            data class Opplysning(val behandlingId: Long, val fødselsdato: LocalDate, val aktiv: Boolean)
            data class Grunnlag(val personopplysningId: Long, val opplysning: Opplysning)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID AS BEHANDLING_ID, p.ID AS PERSONOPPLYSNING_ID, p.FODSELSDATO, g.AKTIV
                    FROM BEHANDLING b
                    INNER JOIN PERSONOPPLYSNING_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN PERSONOPPLYSNING p ON g.PERSONOPPLYSNING_ID = p.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Grunnlag(
                            personopplysningId = row.getLong("PERSONOPPLYSNING_ID"),
                            opplysning = Opplysning(
                                behandlingId = row.getLong("BEHANDLING_ID"),
                                fødselsdato = row.getLocalDate("FODSELSDATO"),
                                aktiv = row.getBoolean("AKTIV")
                            )
                        )
                    }
                }
            assertThat(opplysninger.map(Grunnlag::personopplysningId).distinct())
                .hasSize(2)
            assertThat(opplysninger.map(Grunnlag::opplysning))
                .hasSize(3)
                .containsExactly(
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        fødselsdato = 17 mars 1992,
                        aktiv = false
                    ),
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        fødselsdato = 17 april 1992,
                        aktiv = true
                    ),
                    Opplysning(
                        behandlingId = behandling2.id.toLong(),
                        fødselsdato = 17 april 1992,
                        aktiv = true
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
        return SakOgBehandlingService(connection).finnEllerOpprettBehandling(sak.saksnummer).behandling
    }
}
