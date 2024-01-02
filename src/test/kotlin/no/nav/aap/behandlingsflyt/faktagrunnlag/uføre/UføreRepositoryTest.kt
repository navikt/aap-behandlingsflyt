package no.nav.aap.behandlingsflyt.faktagrunnlag.uføre

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.uføre.Uføre
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.beregning.Prosent
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.sakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class UføreRepositoryTest {

    @Test
    fun `Finner ikke uføre hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = behandling(connection, sak(connection))

            val uføreRepository = UføreRepository(connection)
            val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(uføreGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter uføre`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = behandling(connection, sak(connection))

            val uføreRepository = UføreRepository(connection)
            uføreRepository.lagre(behandling.id, Uføre(Prosent(100)))
            val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(uføreGrunnlag?.vurdering).isEqualTo(Uføre(Prosent(100)))
        }
    }

    @Test
    fun `Lagrer ikke lik uføre flere ganger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val uføreRepository = UføreRepository(connection)
            uføreRepository.lagre(behandling.id, Uføre(Prosent(100)))
            uføreRepository.lagre(behandling.id, Uføre(Prosent(80)))
            uføreRepository.lagre(behandling.id, Uføre(Prosent(80)))

            val opplysninger = connection.queryList(
                """
                    SELECT u.UFOREGRAD
                    FROM BEHANDLING b
                    INNER JOIN UFORE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN UFORE u ON g.UFORE_ID = u.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
            ) {
                setParams {
                    setLong(1, sak.id.toLong())
                }
                setRowMapper { row -> row.getInt("UFOREGRAD") }
            }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(100, 80)
        }
    }

    @Test
    fun `Kopierer uføre fra en behandling til en annen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val uføreRepository = UføreRepository(connection)
            uføreRepository.lagre(behandling1.id, Uføre(Prosent(100)))
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }

            val behandling2 = behandling(connection, sak)

            val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandling2.id)
            assertThat(uføreGrunnlag?.vurdering).isEqualTo(Uføre(Prosent(100)))
        }
    }

    @Test
    fun `Kopiering av uføre fra en behandling uten opplysningene skal ikke føre til feil`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val uføreRepository = UføreRepository(connection)
            assertDoesNotThrow {
                uføreRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Kopierer uføre fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val uføreRepository = UføreRepository(connection)
            uføreRepository.lagre(behandling1.id, Uføre(Prosent(100)))
            uføreRepository.lagre(behandling1.id, Uføre(Prosent(80)))
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }

            val behandling2 = behandling(connection, sak)

            val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandling2.id)
            assertThat(uføreGrunnlag?.vurdering).isEqualTo(Uføre(Prosent(80)))
        }
    }

    @Test
    fun `Lagrer nye uføreopplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)
            val uføreRepository = UføreRepository(connection)

            uføreRepository.lagre(behandling.id, Uføre(Prosent(100)))
            val orginaltGrunnlag = uføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.vurdering).isEqualTo(Uføre(Prosent(100)))

            uføreRepository.lagre(behandling.id, Uføre(Prosent(80)))
            val oppdatertGrunnlag = uføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurdering).isEqualTo(Uføre(Prosent(80)))

            data class Opplysning(
                val aktiv: Boolean,
                val uføregrad: Prosent
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT g.AKTIV, u.UFOREGRAD
                    FROM BEHANDLING b
                    INNER JOIN UFORE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN UFORE u ON g.UFORE_ID = u.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Opplysning(
                            aktiv = row.getBoolean("AKTIV"),
                            uføregrad = Prosent(row.getInt("UFOREGRAD"))
                        )
                    }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(
                    Opplysning(aktiv = false, uføregrad = Prosent(100)),
                    Opplysning(aktiv = true, uføregrad = Prosent(80))
                )
        }
    }

    @Test
    fun `Ved kopiering av uføreopplysninger fra en avsluttet behandling til en ny skal kun referansen kopieres, ikke hele raden`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val uføreRepository = UføreRepository(connection)
            uføreRepository.lagre(behandling1.id, Uføre(Prosent(100)))
            uføreRepository.lagre(behandling1.id, Uføre(Prosent(80)))
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = behandling(connection, sak)

            data class Opplysning(
                val behandlingId: Long,
                val aktiv: Boolean,
                val uføregrad: Prosent
            )

            data class Grunnlag(val uføreId: Long, val opplysning: Opplysning)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID AS BEHANDLING_ID, u.ID AS UFORE_ID, g.AKTIV, u.UFOREGRAD
                    FROM BEHANDLING b
                    INNER JOIN UFORE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN UFORE u ON g.UFORE_ID = u.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Grunnlag(
                            uføreId = row.getLong("UFORE_ID"),
                            opplysning = Opplysning(
                                behandlingId = row.getLong("BEHANDLING_ID"),
                                aktiv = row.getBoolean("AKTIV"),
                                uføregrad = Prosent(row.getInt("UFOREGRAD"))
                            )
                        )
                    }
                }
            assertThat(opplysninger.map(Grunnlag::uføreId).distinct())
                .hasSize(2)
            assertThat(opplysninger.map(Grunnlag::opplysning))
                .hasSize(3)
                .containsExactly(
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = false,
                        uføregrad = Prosent(100)
                    ),
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = true,
                        uføregrad = Prosent(80)
                    ),
                    Opplysning(
                        behandlingId = behandling2.id.toLong(),
                        aktiv = true,
                        uføregrad = Prosent(80)
                    )
                )
        }
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return sakRepository(connection).finnEllerOpprett(
            person = PersonRepository(connection).finnEllerOpprett(ident()),
            periode = periode
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        val behandling = behandlingRepository(connection).finnSisteBehandlingFor(sak.id)
        if (behandling == null || behandling.status().erAvsluttet()) {
            return behandlingRepository(connection).opprettBehandling(sak.id, listOf())
        }
        return behandling
    }
}
