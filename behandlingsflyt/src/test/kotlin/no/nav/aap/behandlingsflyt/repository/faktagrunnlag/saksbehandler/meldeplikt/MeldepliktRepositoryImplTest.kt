package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MeldepliktRepositoryImplTest {
    companion object {
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
    fun `Finner ikke fritaksvurderinger hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val meldepliktRepository = MeldepliktRepositoryImpl(connection)
            val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(meldepliktGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter fritaksvurderinger`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val meldepliktRepository = MeldepliktRepositoryImpl(connection)
            val fritaksvurderinger = listOf(
                Fritaksvurdering(true, 13 august 2023, "en begrunnelse", "saksbehandler", null),
                Fritaksvurdering(false, 26 august 2023, "annen begrunnelse", "saksbehandler", LocalDateTime.now())
            )
            meldepliktRepository.lagre(behandling.id, fritaksvurderinger)
            val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(meldepliktGrunnlag?.vurderinger).hasSize(2)
        }
    }

    @Test
    fun `Kopierer fritaksvurderinger fra en behandling til en annen`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val meldepliktRepository = MeldepliktRepositoryImpl(connection)
            val fritaksvurdering = Fritaksvurdering(true, 13 august 2023, "en begrunnelse", "saksbehandler", null)

            meldepliktRepository.lagre(
                behandling1.id,
                listOf(fritaksvurdering)
            )
            val behandling1Grunnlag =
                meldepliktRepository.hentHvisEksisterer(behandling1.id)?.vurderinger.orEmpty()

            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling2.id)?.vurderinger.orEmpty()
            assertThat(meldepliktGrunnlag).containsExactlyInAnyOrderElementsOf(behandling1Grunnlag)
        }
    }

    @Test
    fun `Kopiering av fritaksvurderinger fra en behandling uten opplysningene skal ikke føre til feil`() {
        val dataSource = dataSource
        dataSource.transaction { connection ->
            val bistandRepository = MeldepliktRepositoryImpl(connection)
            org.junit.jupiter.api.assertDoesNotThrow {
                bistandRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Kopierer fritaksvurderinger fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        val dataSource = dataSource
        val behandling1 = dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val meldepliktRepository = MeldepliktRepositoryImpl(connection)
            val fritaksvurdering = Fritaksvurdering(true, 13 august 2023, "en begrunnelse", "saksbehandler", null)

            meldepliktRepository.lagre(
                behandling1.id,
                listOf(fritaksvurdering)
            )
            meldepliktRepository.lagre(
                behandling1.id,
                listOf(fritaksvurdering.copy(begrunnelse = "annen begrunnelse"))
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
            behandling1
        }
        dataSource.transaction { connection ->
            val meldepliktRepository = MeldepliktRepositoryImpl(connection)
            val sakService = SakService(postgresRepositoryRegistry.provider(connection))
            val sak = sakService.hentSakFor(behandling1.id)
            val behandling2 = finnEllerOpprettBehandling(connection, sak)
            assertThat(behandling1.id).isNotEqualTo(behandling2.id)
            assertThat(behandling1.opprettetTidspunkt).isBefore(behandling2.opprettetTidspunkt)

            val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling2.id)
            assertThat(meldepliktGrunnlag?.vurderinger).hasSize(1)

            val alleVurderingerFørBehandling =
                meldepliktRepository.hentAlleVurderinger(behandling2.sakId, behandling2.id)

            assertThat(alleVurderingerFørBehandling).hasSize(1)
        }
    }

    @Test
    fun `Lagrer nye fritaksvurderinger som nye rader og deaktiverer forrige versjon av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val meldepliktRepository = MeldepliktRepositoryImpl(connection)

            val fritaksvurdering = Fritaksvurdering(true, 13 august 2023, "en begrunnelse", "saksbehandler", null)

            meldepliktRepository.lagre(
                behandling.id,
                listOf(fritaksvurdering)
            )
            val orginaltGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.vurderinger).hasSize(1)

            meldepliktRepository.lagre(
                behandling.id,
                listOf(fritaksvurdering.copy(begrunnelse = "annen begrunnelse"))
            )
            val oppdatertGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurderinger).hasSize(1)

            data class Opplysning(
                val behandlingId: Long,
                val aktiv: Boolean,
                val fraDato: LocalDate,
                val begrunnelse: String,
                val harFritak: Boolean
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT g.BEHANDLING_ID, g.AKTIV, v.BEGRUNNELSE, v.HAR_FRITAK, v.FRA_DATO
                    FROM BEHANDLING b
                    INNER JOIN MELDEPLIKT_FRITAK_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
                    INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
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
                            fraDato = row.getLocalDate("FRA_DATO"),
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
                        fraDato = 13 august 2023,
                        begrunnelse = "en begrunnelse",
                        harFritak = true
                    ),
                    Opplysning(
                        behandlingId = behandling.id.toLong(),
                        aktiv = true,
                        fraDato = 13 august 2023,
                        begrunnelse = "annen begrunnelse",
                        harFritak = true
                    ),
                )

            val alleVurderingerFørBehandling = meldepliktRepository.hentAlleVurderinger(behandling.sakId, behandling.id)

            assertThat(alleVurderingerFørBehandling).isEmpty()
        }
    }

    @Test
    fun `Lagrer nye fritaksvurderinger skal deaktivere forrige grunnlag selv om den ikke har noen vurderinger`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val meldepliktRepository = MeldepliktRepositoryImpl(connection)

            val fritaksvurdering = Fritaksvurdering(true, 13 august 2023, "en begrunnelse", "saksbehandler", null)

            meldepliktRepository.lagre(
                behandling.id,
                emptyList()
            )

            meldepliktRepository.lagre(
                behandling.id,
                listOf(fritaksvurdering)
            )

            val oppdatertGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurderinger).hasSize(1)
            assertThat(oppdatertGrunnlag?.vurderinger?.first()?.harFritak).isTrue()
        }
    }

    @Test
    fun `Ved kopiering av fritaksvurderinger fra en avsluttet behandling til en ny skal kun referansen kopieres, ikke hele raden`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val meldepliktRepository = MeldepliktRepositoryImpl(connection)

            val fritaksvurdering = Fritaksvurdering(true, 13 august 2023, "en begrunnelse", "saksbehandler", null)

            meldepliktRepository.lagre(
                behandling1.id,
                listOf(fritaksvurdering)
            )
            meldepliktRepository.lagre(
                behandling1.id,
                listOf(fritaksvurdering.copy(begrunnelse = "annen begrunnelse"))
            )
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            data class Opplysning(
                val behandlingId: Long,
                val aktiv: Boolean,
                val fraDato: LocalDate,
                val begrunnelse: String,
                val harFritak: Boolean
            )

            data class Grunnlag(val meldepliktId: Long, val opplysning: Opplysning)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID AS BEHANDLING_ID, f.ID AS MELDEPLIKT_ID, g.AKTIV, v.BEGRUNNELSE, v.FRA_DATO, v.HAR_FRITAK
                    FROM BEHANDLING b
                    INNER JOIN MELDEPLIKT_FRITAK_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
                    INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
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
                                fraDato = row.getLocalDate("FRA_DATO"),
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
                        fraDato = 13 august 2023,
                        begrunnelse = "en begrunnelse",
                        harFritak = true
                    ),
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = true,
                        fraDato = 13 august 2023,
                        begrunnelse = "annen begrunnelse",
                        harFritak = true
                    ),
                    Opplysning(
                        behandlingId = behandling2.id.toLong(),
                        aktiv = true,
                        fraDato = 13 august 2023,
                        begrunnelse = "annen begrunnelse",
                        harFritak = true
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