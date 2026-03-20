package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreSøknad
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class UføreSøknadRepositoryImplTest {
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
    fun `Finner ikke uføresøknad hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val uføreSøknadRepository = UføreSøknadRepositoryImpl(connection)
            val uføreSøknadGrunnlag = uføreSøknadRepository.hentHvisEksisterer(behandling.id)
            assertThat(uføreSøknadGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter uføresøknad`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val uføreSøknadRepository = UføreSøknadRepositoryImpl(connection)
            val søknad = UføreSøknad(LocalDate.now(), 1L)
            uføreSøknadRepository.lagre(behandling.id, søknad)
            val uføreSøknadGrunnlag = uføreSøknadRepository.hentHvisEksisterer(behandling.id)
            assertThat(uføreSøknadGrunnlag?.uføreSøknad).isEqualTo(søknad)
        }
    }

    @Test
    fun `Lagrer ikke like søknadsgrunnlag flere ganger`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val uføreSøknadRepository = UføreSøknadRepositoryImpl(connection)
            val søknad = UføreSøknad(LocalDate.now(), 1L)
            uføreSøknadRepository.lagre(behandling.id, søknad)
            uføreSøknadRepository.lagre(behandling.id, søknad)
            uføreSøknadRepository.lagre(behandling.id, søknad)
            val opplysninger =
                connection.queryList(
                    """
                    SELECT usg.AKTIV, usg.SOKNADSDATO
                    FROM BEHANDLING b
                    INNER JOIN UFORE_SOKNAD_GRUNNLAG usg ON b.ID = usg.BEHANDLING_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Pair(
                            row.getBoolean("AKTIV"),
                            row.getLocalDate("SOKNADSDATO")
                        )
                    }
                }
            assertThat(opplysninger).hasSize(1)
        }
    }

    @Test
    fun `Skal ikke kopierer uføresøknader fra en behandling til en annen`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val uføreSøknadRepository = UføreSøknadRepositoryImpl(connection)
            val søknad = UføreSøknad(LocalDate.now(), 1L)
            uføreSøknadRepository.lagre(behandling1.id, søknad)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val uføreSøknadGrunnlag = uføreSøknadRepository.hentHvisEksisterer(behandling2.id)
            assertThat(uføreSøknadGrunnlag).isNull()
        }
    }

    @Test
    fun `test sletting`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val uføreSøknadRepository = UføreSøknadRepositoryImpl(connection)
            val søknad = UføreSøknad(LocalDate.now(), 1L)
            uføreSøknadRepository.lagre(behandling.id, søknad)
            assertDoesNotThrow { uføreSøknadRepository.slett(behandling.id) }
            assertThat(uføreSøknadRepository.hentHvisEksisterer(behandling.id)).isNull()
        }
    }

    @Test
    fun `Lagrer nye uføresøknadsopplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val uføreSøknadRepository = UføreSøknadRepositoryImpl(connection)
            val søknad = UføreSøknad(LocalDate.now(), 1L)
            val endretSøknad = UføreSøknad(LocalDate.now().plusDays(1), 1L)
            val endretSøknadNySak = UføreSøknad(LocalDate.now().plusDays(1), 2L)
            uføreSøknadRepository.lagre(behandling.id, søknad)
            val orginaltGrunnlag = uføreSøknadRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.uføreSøknad).isEqualTo(søknad)

            uføreSøknadRepository.lagre(behandling.id, endretSøknad)
            val oppdatertGrunnlag = uføreSøknadRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.uføreSøknad).isEqualTo(endretSøknad)

            uføreSøknadRepository.lagre(behandling.id, endretSøknadNySak)
            val oppdatertGrunnlagNySak = uføreSøknadRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlagNySak?.uføreSøknad).isEqualTo(endretSøknadNySak)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT usg.AKTIV, usg.SOKNADSDATO
                    FROM BEHANDLING b
                    INNER JOIN UFORE_SOKNAD_GRUNNLAG usg ON b.ID = usg.BEHANDLING_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Pair(
                            row.getBoolean("AKTIV"),
                            row.getLocalDate("SOKNADSDATO")
                        )
                    }
                }
            assertThat(opplysninger)
                .hasSize(3)
                .containsExactly(
                    Pair(false, søknad.soknadsdato),
                    Pair(false, endretSøknad.soknadsdato),
                    Pair(true, endretSøknadNySak.soknadsdato)
                )
        }
    }

}

