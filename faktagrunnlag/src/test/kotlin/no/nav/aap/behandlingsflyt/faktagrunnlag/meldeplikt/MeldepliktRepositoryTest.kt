package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.august
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.LocalDateTime

class MeldepliktRepositoryTest {
    @Test
    fun `Finner ikke fritaksvurderinger hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val meldepliktRepository = MeldepliktRepository(connection)
            val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(meldepliktGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter fritaksvurderinger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val meldepliktRepository = MeldepliktRepository(connection)
            val fritaksvurdering = Fritaksvurdering(
                listOf(
                    Fritaksperiode(Periode(13 august 2023, 25 august 2023), true),
                    Fritaksperiode(Periode(26 august 2023, 31 august 2023), false)
                ), "en begrunnelse", LocalDateTime.now()
            )
            meldepliktRepository.lagre(behandling.id, listOf(fritaksvurdering))
            val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(meldepliktGrunnlag?.vurderinger).containsExactly(fritaksvurdering)
        }
    }

    @Test
    fun `Lagrer ikke like opplysninger flere ganger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val meldepliktRepository = MeldepliktRepository(connection)
            val fritaksvurdering = Fritaksvurdering(
                listOf(
                    Fritaksperiode(Periode(13 august 2023, 25 august 2023), true)
                ), "en begrunnelse", LocalDateTime.now()
            )

            meldepliktRepository.lagre(behandling.id, listOf(fritaksvurdering))
            repeat(2) {
                meldepliktRepository.lagre(behandling.id, listOf(fritaksvurdering.copy(begrunnelse = "annen begrunnelse")))
            }

            val opplysninger =
                connection.queryList(
                    """
                    SELECT v.BEGRUNNELSE 
                    FROM BEHANDLING b
                    INNER JOIN MELDEPLIKT_FRITAK_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
                    INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
                    WHERE b.SAK_ID = ?
                """.trimMargin()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row -> row.getString("BEGRUNNELSE") }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly("en begrunnelse", "annen begrunnelse")
        }
    }

    @Test
    fun `Kopierer fritaksvurderinger fra en behandling til en annen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val meldepliktRepository = MeldepliktRepository(connection)
            val fritaksvurdering = Fritaksvurdering(
                listOf(
                    Fritaksperiode(Periode(13 august 2023, 25 august 2023), true)
                ), "en begrunnelse", LocalDateTime.now()
            )

            meldepliktRepository.lagre(
                behandling1.id,
                listOf(fritaksvurdering)
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = behandling(connection, sak)

            val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling2.id)
            assertThat(meldepliktGrunnlag?.vurderinger).containsExactly(fritaksvurdering)
        }
    }

    @Test
    fun `Kopiering av fritaksvurderinger fra en behandling uten opplysningene skal ikke føre til feil`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val bistandRepository = BistandRepository(connection)
            assertDoesNotThrow {
                bistandRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Kopierer fritaksvurderinger fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val meldepliktRepository = MeldepliktRepository(connection)
            val fritaksvurdering = Fritaksvurdering(
                listOf(
                    Fritaksperiode(Periode(13 august 2023, 25 august 2023), true)
                ), "en begrunnelse", LocalDateTime.now()
            )
            meldepliktRepository.lagre(
                behandling1.id,
                listOf(fritaksvurdering)
            )
            meldepliktRepository.lagre(
                behandling1.id,
                listOf(fritaksvurdering.copy(begrunnelse = "annen begrunnelse"))
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = behandling(connection, sak)

            val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling2.id)
            assertThat(meldepliktGrunnlag?.vurderinger).containsExactly(fritaksvurdering.copy(begrunnelse = "annen begrunnelse"))
        }
    }

    @Test
    fun `Lagrer nye fritaksvurderinger som nye rader og deaktiverer forrige versjon av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)
            val meldepliktRepository = MeldepliktRepository(connection)

            val fritaksvurdering = Fritaksvurdering(
                listOf(
                    Fritaksperiode(Periode(13 august 2023, 25 august 2023), true)
                ), "en begrunnelse", LocalDateTime.now()
            )

            meldepliktRepository.lagre(
                behandling.id,
                listOf(fritaksvurdering)
            )
            val orginaltGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.vurderinger).containsExactly(fritaksvurdering)

            meldepliktRepository.lagre(
                behandling.id,
                listOf(fritaksvurdering.copy(begrunnelse = "annen begrunnelse"))
            )
            val oppdatertGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurderinger).containsExactly(fritaksvurdering.copy(begrunnelse = "annen begrunnelse"))

            data class Opplysning(
                val behandlingId: Long,
                val aktiv: Boolean,
                val periode: Periode,
                val begrunnelse: String,
                val harFritak: Boolean
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT g.BEHANDLING_ID, g.AKTIV, v.BEGRUNNELSE, p.PERIODE, p.HAR_FRITAK
                    FROM BEHANDLING b
                    INNER JOIN MELDEPLIKT_FRITAK_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
                    INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
                    INNER JOIN MELDEPLIKT_FRITAK_PERIODE p ON v.ID = p.VURDERING_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Opplysning(
                            behandlingId = row.getLong("BEHANDLING_ID"),
                            aktiv = row.getBoolean("AKTIV"),
                            periode = row.getPeriode("PERIODE"),
                            begrunnelse = row.getString("BEGRUNNELSE"),
                            harFritak = row.getBoolean("HAR_FRITAK")
                        )
                    }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(
                    Opplysning(
                        behandlingId = behandling.id.toLong(),
                        aktiv = false,
                        periode = Periode(13 august 2023, 25 august 2023),
                        begrunnelse = "en begrunnelse",
                        harFritak = true
                    ),
                    Opplysning(
                        behandlingId = behandling.id.toLong(),
                        aktiv = true,
                        periode = Periode(13 august 2023, 25 august 2023),
                        begrunnelse = "annen begrunnelse",
                        harFritak = true
                    ),
                )
        }
    }

    @Test
    fun `Ved kopiering av fritaksvurderinger fra en avsluttet behandling til en ny skal kun referansen kopieres, ikke hele raden`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val meldepliktRepository = MeldepliktRepository(connection)

            val fritaksvurdering = Fritaksvurdering(
                listOf(
                    Fritaksperiode(Periode(13 august 2023, 25 august 2023), true)
                ), "en begrunnelse", LocalDateTime.now()
            )

            meldepliktRepository.lagre(
                behandling1.id,
                listOf(fritaksvurdering)
            )
            meldepliktRepository.lagre(
                behandling1.id,
                listOf(fritaksvurdering.copy(begrunnelse = "annen begrunnelse"))
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = behandling(connection, sak)

            data class Opplysning(
                val behandlingId: Long,
                val aktiv: Boolean,
                val periode: Periode,
                val begrunnelse: String,
                val harFritak: Boolean
            )

            data class Grunnlag(val meldepliktId: Long, val opplysning: Opplysning)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID AS BEHANDLING_ID, f.ID AS MELDEPLIKT_ID, g.AKTIV, v.BEGRUNNELSE, p.PERIODE, p.HAR_FRITAK
                    FROM BEHANDLING b
                    INNER JOIN MELDEPLIKT_FRITAK_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
                    INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
                    INNER JOIN MELDEPLIKT_FRITAK_PERIODE p ON v.ID = p.VURDERING_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Grunnlag(
                            meldepliktId = row.getLong("MELDEPLIKT_ID"),
                            opplysning = Opplysning(
                                behandlingId = row.getLong("BEHANDLING_ID"),
                                aktiv = row.getBoolean("AKTIV"),
                                periode = row.getPeriode("PERIODE"),
                                begrunnelse = row.getString("BEGRUNNELSE"),
                                harFritak = row.getBoolean("HAR_FRITAK")
                            )
                        )
                    }
                }
            assertThat(opplysninger.map(Grunnlag::meldepliktId).distinct())
                .hasSize(2)
            assertThat(opplysninger.map(Grunnlag::opplysning))
                .hasSize(3)
                .containsExactly(
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = false,
                        periode = Periode(13 august 2023, 25 august 2023),
                        begrunnelse = "en begrunnelse",
                        harFritak = true
                    ),
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = true,
                        periode = Periode(13 august 2023, 25 august 2023),
                        begrunnelse = "annen begrunnelse",
                        harFritak = true
                    ),
                    Opplysning(
                        behandlingId = behandling2.id.toLong(),
                        aktiv = true,
                        periode = Periode(13 august 2023, 25 august 2023),
                        begrunnelse = "annen begrunnelse",
                        harFritak = true
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
