package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class PersonopplysningRepositoryImplTest {
    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
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
    fun `Finner ikke personopplysninger hvis ikke lagret`() {
        val behandling = dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
        }

        val personopplysningGrunnlag = dataSource.transaction { connection ->
            PersonopplysningRepositoryImpl(
                connection
            ).hentHvisEksisterer(behandling.id)
        }
        assertThat(personopplysningGrunnlag).isNull()
    }

    @Test
    fun `Lagrer og henter personopplysninger`() {
        val behandling = dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
        }

        dataSource.transaction { connection ->
            val personopplysningRepository = PersonopplysningRepositoryImpl(
                connection
            )
            personopplysningRepository.lagre(
                behandling.id, Personopplysning(
                    Fødselsdato(17 mars 1992), statsborgerskap = listOf(
                        Statsborgerskap("NOR")
                    ), status = PersonStatus.bosatt
                )
            )
        }
        val personopplysningGrunnlag = dataSource.transaction { connection ->
            val personopplysningRepository = PersonopplysningRepositoryImpl(
                connection
            )
            personopplysningRepository.hentHvisEksisterer(behandling.id)
        }
        assertThat(personopplysningGrunnlag?.brukerPersonopplysning).isEqualTo(
            Personopplysning(
                Fødselsdato(17 mars 1992),
                statsborgerskap = listOf(Statsborgerskap("NOR")),
                status = PersonStatus.bosatt
            )
        )
    }

    @Test
    fun `Lagrer ikke like opplysninger flere ganger`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val personopplysningRepository = PersonopplysningRepositoryImpl(
                connection
            )
            personopplysningRepository.lagre(
                behandling.id,
                Personopplysning(
                    Fødselsdato(17 mars 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )
            personopplysningRepository.lagre(
                behandling.id,
                Personopplysning(
                    Fødselsdato(18 mars 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )
            personopplysningRepository.lagre(
                behandling.id,
                Personopplysning(
                    Fødselsdato(18 mars 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT p.FODSELSDATO
                    FROM BEHANDLING b
                    INNER JOIN PERSONOPPLYSNING_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN BRUKER_PERSONOPPLYSNING p ON g.Bruker_PERSONOPPLYSNING_ID = p.ID
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
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val personopplysningRepository = PersonopplysningRepositoryImpl(
                connection
            )
            personopplysningRepository.lagre(
                behandling1.id,
                Personopplysning(
                    Fødselsdato(17 mars 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling2.id)
            assertThat(personopplysningGrunnlag?.brukerPersonopplysning).isEqualTo(
                Personopplysning(
                    Fødselsdato(17 mars 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )
        }
    }

    @Test
    fun `Kopiering av personopplysninger fra en behandling uten opplysningene skal ikke føre til feil`() {
        dataSource.transaction { connection ->
            val personopplysningRepository = PersonopplysningRepositoryImpl(
                connection
            )
            assertDoesNotThrow {
                personopplysningRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Kopierer personopplysninger fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val personopplysningRepository = PersonopplysningRepositoryImpl(
                connection
            )
            personopplysningRepository.lagre(
                behandling1.id,
                Personopplysning(
                    Fødselsdato(16 mars 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )
            personopplysningRepository.lagre(
                behandling1.id,
                Personopplysning(
                    Fødselsdato(17 mars 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling2.id)
            assertThat(personopplysningGrunnlag?.brukerPersonopplysning).isEqualTo(
                Personopplysning(
                    Fødselsdato(17 mars 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )
        }
    }

    @Test
    fun `Lagrer nye opplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val personopplysningRepository = PersonopplysningRepositoryImpl(
                connection
            )

            personopplysningRepository.lagre(
                behandling.id,
                Personopplysning(
                    Fødselsdato(17 mars 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )
            val orginaltGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.brukerPersonopplysning).isEqualTo(
                Personopplysning(
                    Fødselsdato(17 mars 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )

            personopplysningRepository.lagre(
                behandling.id,
                Personopplysning(
                    Fødselsdato(18 mars 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )
            val oppdatertGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.brukerPersonopplysning).isEqualTo(
                Personopplysning(
                    Fødselsdato(18 mars 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )

            data class Opplysning(val behandlingId: Long, val fødselsdato: LocalDate, val aktiv: Boolean)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID, p.FODSELSDATO, g.AKTIV
                    FROM BEHANDLING b
                    INNER JOIN PERSONOPPLYSNING_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN BRUKER_PERSONOPPLYSNING p ON g.BRUKER_PERSONOPPLYSNING_ID = p.ID
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
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val personopplysningRepository = PersonopplysningRepositoryImpl(
                connection
            )
            personopplysningRepository.lagre(
                behandling1.id,
                Personopplysning(
                    Fødselsdato(17 mars 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )
            personopplysningRepository.lagre(
                behandling1.id,
                Personopplysning(
                    Fødselsdato(17 april 1992),
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    status = PersonStatus.bosatt
                )
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            data class Opplysning(val behandlingId: Long, val fødselsdato: LocalDate, val aktiv: Boolean)
            data class Grunnlag(val personopplysningId: Long, val opplysning: Opplysning)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID AS BEHANDLING_ID, p.ID AS PERSONOPPLYSNING_ID, p.FODSELSDATO, g.AKTIV
                    FROM BEHANDLING b
                    INNER JOIN PERSONOPPLYSNING_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN BRUKER_PERSONOPPLYSNING p ON g.BRUKER_PERSONOPPLYSNING_ID = p.ID
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

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }
}
