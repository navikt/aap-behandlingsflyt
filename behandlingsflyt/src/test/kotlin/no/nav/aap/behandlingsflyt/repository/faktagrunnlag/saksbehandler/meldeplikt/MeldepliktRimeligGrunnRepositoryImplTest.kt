package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.RimeligGrunnVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class MeldepliktRimeligGrunnRepositoryImplTest {
    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `Finner ikke rimeligGrunnVurderinger hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val meldepliktRimeligGrunnRepository = MeldepliktRimeligGrunnRepositoryImpl(connection)
            val meldepliktRimeligGrunnGrunnlag = meldepliktRimeligGrunnRepository.hentHvisEksisterer(behandling.id)
            assertThat(meldepliktRimeligGrunnGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter rimeligGrunnVurderinger`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val meldepliktRimeligGrunnRepository = MeldepliktRimeligGrunnRepositoryImpl(connection)
            val rimeligGrunnVurderinger = listOf(
                RimeligGrunnVurdering(true, 13 august 2023, "en begrunnelse", "saksbehandler", null),
                RimeligGrunnVurdering(false, 26 august 2023, "annen begrunnelse", "saksbehandler", null)
            )
            meldepliktRimeligGrunnRepository.lagre(behandling.id, rimeligGrunnVurderinger)
            val meldepliktRimeligGrunnGrunnlag = meldepliktRimeligGrunnRepository.hentHvisEksisterer(behandling.id)
            assertThat(meldepliktRimeligGrunnGrunnlag?.vurderinger).hasSize(2)
        }
    }

    @Test
    fun `Kopierer rimeligGrunnVurderinger fra en behandling til en annen`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val meldepliktRimeligGrunnRepository = MeldepliktRimeligGrunnRepositoryImpl(connection)
            val rimeligGrunnVurdering = RimeligGrunnVurdering(true, 13 august 2023, "en begrunnelse", "saksbehandler", null)

            meldepliktRimeligGrunnRepository.lagre(
                behandling1.id,
                listOf(rimeligGrunnVurdering)
            )
            val behandling1Grunnlag =
                meldepliktRimeligGrunnRepository.hentHvisEksisterer(behandling1.id)?.vurderinger ?: emptyList()
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val meldepliktRimeligGrunnGrunnlag = meldepliktRimeligGrunnRepository.hentHvisEksisterer(behandling2.id)?.vurderinger ?: emptyList()
            assertThat(meldepliktRimeligGrunnGrunnlag).containsExactlyInAnyOrderElementsOf(behandling1Grunnlag)
        }
    }

    @Test
    fun `Kopiering av rimeligGrunnVurderinger fra en behandling uten opplysningene skal ikke føre til feil`() {
        val dataSource = dataSource
        dataSource.transaction { connection ->
            val bistandRepository = MeldepliktRimeligGrunnRepositoryImpl(connection)
            assertDoesNotThrow {
                bistandRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Kopierer rimeligGrunnVurderinger fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        val dataSource = dataSource
        val behandling1 = dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val meldepliktRimeligGrunnRepository = MeldepliktRimeligGrunnRepositoryImpl(connection)
            val rimeligGrunnVurdering = RimeligGrunnVurdering(true, 13 august 2023, "en begrunnelse", "saksbehandler", null)

            meldepliktRimeligGrunnRepository.lagre(
                behandling1.id,
                listOf(rimeligGrunnVurdering)
            )
            meldepliktRimeligGrunnRepository.lagre(
                behandling1.id,
                listOf(rimeligGrunnVurdering.copy(begrunnelse = "annen begrunnelse"))
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            behandling1
        }
        dataSource.transaction { connection ->
            val meldepliktRimeligGrunnRepository = MeldepliktRimeligGrunnRepositoryImpl(connection)
            val sakOgBehandlingService = SakOgBehandlingService(
                postgresRepositoryRegistry.provider(connection),
                unleashGateway = FakeUnleash,
            )
            val sak = sakOgBehandlingService.hentSakFor(behandling1.id)
            val behandling2 = finnEllerOpprettBehandling(connection, sak)
            assertThat(behandling1.id).isNotEqualTo(behandling2.id)
            assertThat(behandling1.opprettetTidspunkt).isBefore(behandling2.opprettetTidspunkt)

            val meldepliktRimeligGrunnGrunnlag = meldepliktRimeligGrunnRepository.hentHvisEksisterer(behandling2.id)
            assertThat(meldepliktRimeligGrunnGrunnlag?.vurderinger).hasSize(1)

            val alleVurderingerFørBehandling =
                meldepliktRimeligGrunnRepository.hentAlleVurderinger(behandling2.sakId, behandling2.id)

            assertThat(alleVurderingerFørBehandling).hasSize(1)
        }
    }

    @Test
    fun `Lagrer nye rimeligGrunnVurderinger som nye rader og deaktiverer forrige versjon av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val meldepliktRimeligGrunnRepository = MeldepliktRimeligGrunnRepositoryImpl(connection)

            val rimeligGrunnVurdering = RimeligGrunnVurdering(true, 13 august 2023, "en begrunnelse", "saksbehandler", null)

            meldepliktRimeligGrunnRepository.lagre(
                behandling.id,
                listOf(rimeligGrunnVurdering)
            )
            val orginaltGrunnlag = meldepliktRimeligGrunnRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.vurderinger).hasSize(1)

            meldepliktRimeligGrunnRepository.lagre(
                behandling.id,
                listOf(rimeligGrunnVurdering.copy(begrunnelse = "annen begrunnelse"))
            )
            val oppdatertGrunnlag = meldepliktRimeligGrunnRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurderinger).hasSize(1)

            data class Opplysning(
                val behandlingId: Long,
                val aktiv: Boolean,
                val fraDato: LocalDate,
                val begrunnelse: String,
                val harRimeligGrunn: Boolean
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT g.BEHANDLING_ID, g.AKTIV, v.BEGRUNNELSE, v.HAR_RIMELIG_GRUNN, v.FRA_DATO
                    FROM BEHANDLING b
                    INNER JOIN MELDEPLIKT_RIMELIG_GRUNN_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN MELDEPLIKT_RIMELIG_GRUNN rg ON g.MELDEPLIKT_RIMELIG_GRUNN_ID = rg.ID
                    INNER JOIN MELDEPLIKT_RIMELIG_GRUNN_VURDERING v ON rg.ID = v.MELDEPLIKT_RIMELIG_GRUNN_ID
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
                            harRimeligGrunn = row.getBoolean("HAR_RIMELIG_GRUNN")
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
                        harRimeligGrunn = true
                    ),
                    Opplysning(
                        behandlingId = behandling.id.toLong(),
                        aktiv = true,
                        fraDato = 13 august 2023,
                        begrunnelse = "annen begrunnelse",
                        harRimeligGrunn = true
                    ),
                )

            val alleVurderingerFørBehandling = meldepliktRimeligGrunnRepository.hentAlleVurderinger(behandling.sakId, behandling.id)

            assertThat(alleVurderingerFørBehandling).isEmpty()
        }
    }

    @Test
    fun `Ved kopiering av rimeligGrunnVurderinger fra en avsluttet behandling til en ny skal kun referansen kopieres, ikke hele raden`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val meldepliktRimeligGrunnRepository = MeldepliktRimeligGrunnRepositoryImpl(connection)

            val rimeligGrunnVurdering =
                RimeligGrunnVurdering(true, 13 august 2023, "en begrunnelse", "saksbehandler", null)

            meldepliktRimeligGrunnRepository.lagre(
                behandling1.id,
                listOf(rimeligGrunnVurdering)
            )
            meldepliktRimeligGrunnRepository.lagre(
                behandling1.id,
                listOf(rimeligGrunnVurdering.copy(begrunnelse = "annen begrunnelse"))
            )
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            data class Opplysning(
                val behandlingId: Long,
                val aktiv: Boolean,
                val fraDato: LocalDate,
                val begrunnelse: String,
                val harRimeligGrunn: Boolean
            )

            data class RimeligGrunnGrunnlag(val meldepliktRimeligGrunnId: Long, val opplysning: Opplysning)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID AS BEHANDLING_ID, rg.ID AS MELDEPLIKT_RIMELIG_GRUNN_ID, g.AKTIV, v.BEGRUNNELSE, v.FRA_DATO, v.HAR_RIMELIG_GRUNN
                    FROM BEHANDLING b
                    INNER JOIN MELDEPLIKT_RIMELIG_GRUNN_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN MELDEPLIKT_RIMELIG_GRUNN rg ON g.MELDEPLIKT_RIMELIG_GRUNN_ID = rg.ID
                    INNER JOIN MELDEPLIKT_RIMELIG_GRUNN_VURDERING v ON rg.ID = v.MELDEPLIKT_RIMELIG_GRUNN_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        RimeligGrunnGrunnlag(
                            meldepliktRimeligGrunnId = row.getLong("MELDEPLIKT_RIMELIG_GRUNN_ID"),
                            opplysning = Opplysning(
                                behandlingId = row.getLong("BEHANDLING_ID"),
                                aktiv = row.getBoolean("AKTIV"),
                                fraDato = row.getLocalDate("FRA_DATO"),
                                begrunnelse = row.getString("BEGRUNNELSE"),
                                harRimeligGrunn = row.getBoolean("HAR_RIMELIG_GRUNN")
                            )
                        )
                    }
                }
            assertThat(opplysninger.map(RimeligGrunnGrunnlag::meldepliktRimeligGrunnId).distinct())
                .hasSize(2)
            assertThat(opplysninger.map(RimeligGrunnGrunnlag::opplysning))
                .hasSize(3)
                .containsExactly(
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = false,
                        fraDato = 13 august 2023,
                        begrunnelse = "en begrunnelse",
                        harRimeligGrunn = true
                    ),
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = true,
                        fraDato = 13 august 2023,
                        begrunnelse = "annen begrunnelse",
                        harRimeligGrunn = true
                    ),
                    Opplysning(
                        behandlingId = behandling2.id.toLong(),
                        aktiv = true,
                        fraDato = 13 august 2023,
                        begrunnelse = "annen begrunnelse",
                        harRimeligGrunn = true
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