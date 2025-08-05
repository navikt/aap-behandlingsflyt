package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class YrkesskadeRepositoryImplTest {
    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        private val source = InitTestDatabase.freshDatabase()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            InitTestDatabase.closerFor(source)
        }
    }

    @Test
    fun `Finner ikke yrkesskadeopplysninger hvis ikke lagret`() {
        source.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(yrkesskadeGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter yrkesskadeopplysninger`() {
        source.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019)))
            )
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(yrkesskadeGrunnlag?.yrkesskader).isEqualTo(
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019)))
            )
        }
    }

    @Test
    fun `Lagrer ikke like opplysninger flere ganger`() {
        source.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019)))
            )
            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 mai 2019)))
            )
            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 mai 2019)))
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID, g.AKTIV, p.REFERANSE, p.SKADEDATO
                    FROM BEHANDLING b
                    INNER JOIN YRKESSKADE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN YRKESSKADE y ON g.YRKESSKADE_ID = y.ID
                    INNER JOIN YRKESSKADE_DATO p ON y.ID = p.YRKESSKADE_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row -> row.getLocalDate("SKADEDATO") }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(4 juni 2019, 4 mai 2019)
        }
    }

    @Test
    fun `Kopierer yrkesskadeopplysninger fra en behandling til en annen`() {
        source.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling1.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019)))
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling2.id)
            assertThat(yrkesskadeGrunnlag?.yrkesskader).isEqualTo(
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019)))
            )
        }
    }

    @Test
    fun `Kopiering av yrkesskadeopplysninger fra en behandling uten opplysningene skal ikke føre til feil`() {
        source.transaction { connection ->
            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            assertDoesNotThrow {
                yrkesskadeRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Kopierer yrkesskadeopplysninger fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        source.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling1.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019)))
            )
            yrkesskadeRepository.lagre(
                behandling1.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 mai 2019)))
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling2.id)
            assertThat(yrkesskadeGrunnlag?.yrkesskader).isEqualTo(
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 mai 2019)))
            )
        }
    }

    @Test
    fun `test sletting`() {
        source.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019)))
            )

            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(
                    listOf(
                        Yrkesskade(ref = "rexxf", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 5 juni 2019),
                        Yrkesskade(ref = "rxef", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 8 mai 2019)
                    )
                )
            )

            assertDoesNotThrow { yrkesskadeRepository.slett(behandling.id) }
        }
    }

    @Test
    fun `Lagrer nye opplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        source.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)

            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019)))
            )
            val orginaltGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.yrkesskader).isEqualTo(
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019)))
            )

            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 mai 2019)))
            )
            val oppdatertGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.yrkesskader).isEqualTo(
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 mai 2019)))
            )

            data class Opplysning(val behandlingId: Long, val aktiv: Boolean, val ref: String, val skadedato: LocalDate)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID, g.AKTIV, p.REFERANSE, p.SKADEDATO
                    FROM BEHANDLING b
                    INNER JOIN YRKESSKADE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN YRKESSKADE y ON g.YRKESSKADE_ID = y.ID
                    INNER JOIN YRKESSKADE_DATO p ON y.ID = p.YRKESSKADE_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Opplysning(
                            behandlingId = row.getLong("ID"),
                            aktiv = row.getBoolean("AKTIV"),
                            ref = row.getString("REFERANSE"),
                            skadedato = row.getLocalDate("SKADEDATO")
                        )
                    }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(
                    Opplysning(behandling.id.toLong(), false, "ref", 4 juni 2019),
                    Opplysning(behandling.id.toLong(), true, "ref", 4 mai 2019)
                )
        }
    }

    @Test
    fun `Ved kopiering av fritaksvurderinger fra en avsluttet behandling til en ny skal kun referansen kopieres, ikke hele raden`() {
        source.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val yrkesskadeRepository = YrkesskadeRepositoryImpl(connection)
            yrkesskadeRepository.lagre(
                behandling1.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 juni 2019)))
            )
            yrkesskadeRepository.lagre(
                behandling1.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", saksnummer = 123, kildesystem = "INFOTRYGD", skadedato = 4 mai 2019)))
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            data class Opplysning(val behandlingId: Long, val aktiv: Boolean, val ref: String, val skadedato: LocalDate)
            data class Grunnlag(val yrkesskadeId: Long, val opplysning: Opplysning)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID AS BEHANDLING_ID, y.ID AS YRKESSKADE_ID, g.AKTIV, p.REFERANSE, p.SKADEDATO
                    FROM BEHANDLING b
                    INNER JOIN YRKESSKADE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN YRKESSKADE y ON g.YRKESSKADE_ID = y.ID
                    INNER JOIN YRKESSKADE_DATO p ON y.ID = p.YRKESSKADE_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Grunnlag(
                            yrkesskadeId = row.getLong("YRKESSKADE_ID"),
                            opplysning = Opplysning(
                                behandlingId = row.getLong("BEHANDLING_ID"),
                                aktiv = row.getBoolean("AKTIV"),
                                ref = row.getString("REFERANSE"),
                                skadedato = row.getLocalDate("SKADEDATO")
                            )
                        )
                    }
                }
            assertThat(opplysninger.map(Grunnlag::yrkesskadeId).distinct())
                .hasSize(2)
            assertThat(opplysninger.map(Grunnlag::opplysning))
                .hasSize(3)
                .containsExactly(
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = false,
                        ref = "ref",
                        skadedato = 4 juni 2019
                    ),
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = true,
                        ref = "ref",
                        skadedato = 4 mai 2019
                    ),
                    Opplysning(
                        behandlingId = behandling2.id.toLong(),
                        aktiv = true,
                        ref = "ref",
                        skadedato = 4 mai 2019
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
