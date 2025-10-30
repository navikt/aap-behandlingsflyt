package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UføreRepositoryImplTest {
    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `Finner ikke uføre hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val uføreRepository = UføreRepositoryImpl(connection)
            val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(uføreGrunnlag).isNull()

            val eldsteGrunlag = uføreRepository.hentEldsteGrunnlag(behandling.id)
            assertThat(eldsteGrunlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter uføre`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val uføreRepository = UføreRepositoryImpl(connection)
            uføreRepository.lagre(behandling.id, listOf(Uføre(LocalDate.now(), Prosent(100))))
            val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(uføreGrunnlag?.vurderinger).isEqualTo(listOf(Uføre(LocalDate.now(), Prosent(100))))

            val eldsteGrunnlag = uføreRepository.hentEldsteGrunnlag(behandling.id)
            assertThat(eldsteGrunnlag).isNotNull
            assertThat(eldsteGrunnlag).isEqualTo(uføreGrunnlag)
        }
    }

    @Test
    fun `Lagrer ikke lik uføre flere ganger`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val uføreRepository = UføreRepositoryImpl(connection)
            uføreRepository.lagre(behandling.id, listOf(Uføre(LocalDate.now(), Prosent(100))))
            uføreRepository.lagre(behandling.id, listOf(Uføre(LocalDate.now(), Prosent(80))))
            uføreRepository.lagre(behandling.id, listOf(Uføre(LocalDate.now(), Prosent(80))))

            val opplysninger = connection.queryList(
                """
                    SELECT ug.UFOREGRAD
                    FROM BEHANDLING b
                    INNER JOIN UFORE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN UFORE u ON g.UFORE_ID = u.ID
                    INNER JOIN UFORE_GRADERING ug ON ug.UFORE_ID = u.ID
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
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val uføreRepository = UføreRepositoryImpl(connection)
            uføreRepository.lagre(behandling1.id, listOf(Uføre(LocalDate.now(), Prosent(100))))
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandling2.id)
            assertThat(uføreGrunnlag?.vurderinger).isEqualTo(listOf(Uføre(LocalDate.now(), Prosent(100))))
        }
    }

    @Test
    fun `test sletting`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val uføreRepository = UføreRepositoryImpl(connection)
            uføreRepository.lagre(behandling.id, listOf(Uføre(LocalDate.now(), Prosent(100))))
            uføreRepository.lagre(behandling.id, listOf(Uføre(LocalDate.now(), Prosent(50))))
            assertDoesNotThrow { uføreRepository.slett(behandling.id) }
        }
    }

    @Test
    fun `Kopiering av uføre fra en behandling uten opplysningene skal ikke føre til feil`() {
        dataSource.transaction { connection ->
            val uføreRepository = UføreRepositoryImpl(connection)
            assertDoesNotThrow {
                uføreRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Kopierer uføre fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val uføreRepository = UføreRepositoryImpl(connection)
            uføreRepository.lagre(behandling1.id, listOf(Uføre(LocalDate.now(), Prosent(100))))
            uføreRepository.lagre(behandling1.id, listOf(Uføre(LocalDate.now(), Prosent(80))))
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandling2.id)
            assertThat(uføreGrunnlag?.vurderinger).isEqualTo(listOf(Uføre(LocalDate.now(), Prosent(80))))
        }
    }

    @Test
    fun `Lagrer nye uføreopplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val uføreRepository = UføreRepositoryImpl(connection)

            uføreRepository.lagre(behandling.id, listOf(Uføre(LocalDate.now(), Prosent(100))))
            val orginaltGrunnlag = uføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.vurderinger).isEqualTo(listOf(Uføre(LocalDate.now(), Prosent(100))))

            uføreRepository.lagre(behandling.id, listOf(Uføre(LocalDate.now(), Prosent(80))))
            val oppdatertGrunnlag = uføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurderinger).isEqualTo(listOf(Uføre(LocalDate.now(), Prosent(80))))

            val eldsteGrunnlag = uføreRepository.hentEldsteGrunnlag(behandling.id)
            assertThat(eldsteGrunnlag).isEqualTo(orginaltGrunnlag)
            assertThat(eldsteGrunnlag).isNotEqualTo(oppdatertGrunnlag)

            data class Opplysning(
                val aktiv: Boolean,
                val uføregrad: Prosent
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT g.AKTIV, ug.UFOREGRAD
                    FROM BEHANDLING b
                    INNER JOIN UFORE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN UFORE u ON g.UFORE_ID = u.ID
                    INNER JOIN UFORE_GRADERING ug ON ug.UFORE_ID = u.ID
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
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val uføreRepository = UføreRepositoryImpl(connection)
            uføreRepository.lagre(behandling1.id, listOf(Uføre(LocalDate.now(), Prosent(100))))
            uføreRepository.lagre(behandling1.id, listOf(Uføre(LocalDate.now(), Prosent(80))))
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            data class Opplysning(
                val behandlingId: Long,
                val aktiv: Boolean,
                val uføregrad: Prosent
            )

            data class Grunnlag(val uføreId: Long, val opplysning: Opplysning)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID AS BEHANDLING_ID, u.ID AS UFORE_ID, g.AKTIV, ug.UFOREGRAD
                    FROM BEHANDLING b
                    INNER JOIN UFORE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN UFORE u ON g.UFORE_ID = u.ID
                    INNER JOIN UFORE_GRADERING ug ON ug.UFORE_ID = u.ID
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
                .containsExactlyInAnyOrder(
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

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }
}
