package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.august
import no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.SakRepository
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldepliktRepositoryTest {

    private companion object {
        private val ident = Ident("123123123124")
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private lateinit var sak: Sak

    @BeforeEach
    fun setup() {
        InitTestDatabase.dataSource.transaction { connection ->
            sak = SakRepository(connection)
                .finnEllerOpprett(PersonRepository(connection).finnEllerOpprett(ident), periode)
        }
    }

    @AfterEach
    fun tilbakestill() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute("TRUNCATE TABLE SAK, MELDEPLIKT_FRITAK CASCADE")
        }
    }

    @Test
    fun `Finner ikke fritaksvurderinger hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val meldepliktRepository = MeldepliktRepository(connection)
            val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(meldepliktGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter fritaksvurderinger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val meldepliktRepository = MeldepliktRepository(connection)
            meldepliktRepository.lagre(
                behandling.id, listOf(
                    Fritaksvurdering(
                        periode = Periode(13 august 2023, 25 august 2023),
                        begrunnelse = "en begrunnelse",
                        harFritak = true
                    ),
                    Fritaksvurdering(
                        periode = Periode(26 august 2023, 31 august 2023),
                        begrunnelse = "annen begrunnelse",
                        harFritak = false
                    )
                )
            )
            val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(meldepliktGrunnlag?.vurderinger)
                .containsExactly(
                    Fritaksvurdering(
                        periode = Periode(13 august 2023, 25 august 2023),
                        begrunnelse = "en begrunnelse",
                        harFritak = true
                    ),
                    Fritaksvurdering(
                        periode = Periode(26 august 2023, 31 august 2023),
                        begrunnelse = "annen begrunnelse",
                        harFritak = false
                    )
                )
        }
    }

    @Test
    fun `Lagrer ikke like opplysninger flere ganger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val meldepliktRepository = MeldepliktRepository(connection)
            meldepliktRepository.lagre(
                behandling.id,
                listOf(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "en begrunnelse", true))
            )
            meldepliktRepository.lagre(
                behandling.id,
                listOf(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "annen begrunnelse", true))
            )
            meldepliktRepository.lagre(
                behandling.id,
                listOf(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "annen begrunnelse", true))
            )

            val opplysninger =
                connection.queryList("SELECT BEGRUNNELSE FROM MELDEPLIKT_FRITAK_VURDERING") {
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
            val behandling1 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")
            val behandling2 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val meldepliktRepository = MeldepliktRepository(connection)
            meldepliktRepository.lagre(
                behandling1.id,
                listOf(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "en begrunnelse", true))
            )
            meldepliktRepository.kopier(
                fraBehandling = behandling1.id,
                tilBehandling = behandling2.id
            )
            val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling2.id)
            assertThat(meldepliktGrunnlag?.vurderinger)
                .containsExactly(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "en begrunnelse", true))
        }
    }

    @Test
    fun `Kopierer fritaksvurderinger fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling1 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")
            val behandling2 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val meldepliktRepository = MeldepliktRepository(connection)
            meldepliktRepository.lagre(
                behandling1.id,
                listOf(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "en begrunnelse", true))
            )
            meldepliktRepository.lagre(
                behandling1.id,
                listOf(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "annen begrunnelse", true))
            )
            meldepliktRepository.kopier(
                fraBehandling = behandling1.id,
                tilBehandling = behandling2.id
            )
            val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling2.id)
            assertThat(meldepliktGrunnlag?.vurderinger)
                .containsExactly(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "annen begrunnelse", true))
        }
    }

    @Test
    fun `Lagrer nye fritaksvurderinger som nye rader og deaktiverer forrige versjon av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
            val meldepliktRepository = MeldepliktRepository(connection)

            meldepliktRepository.lagre(
                behandling.id,
                listOf(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "en begrunnelse", true))
            )
            val orginaltGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.vurderinger)
                .containsExactly(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "en begrunnelse", true))

            meldepliktRepository.lagre(
                behandling.id,
                listOf(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "annen begrunnelse", true))
            )
            val oppdatertGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurderinger)
                .containsExactly(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "annen begrunnelse", true))

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
                    SELECT g.BEHANDLING_ID, g.AKTIV, v.PERIODE, v.BEGRUNNELSE, v.HAR_FRITAK
                    FROM MELDEPLIKT_FRITAK_GRUNNLAG g
                    INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON g.MELDEPLIKT_ID = v.MELDEPLIKT_ID
                    """.trimIndent()
                ) {
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
            val behandling1 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")
            val behandling2 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val meldepliktRepository = MeldepliktRepository(connection)
            meldepliktRepository.lagre(
                behandling1.id,
                listOf(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "en begrunnelse", true))
            )
            meldepliktRepository.lagre(
                behandling1.id,
                listOf(Fritaksvurdering(Periode(13 august 2023, 25 august 2023), "annen begrunnelse", true))
            )
            meldepliktRepository.kopier(
                fraBehandling = behandling1.id,
                tilBehandling = behandling2.id
            )

            val vurderinger = connection.queryList("SELECT ID FROM MELDEPLIKT_FRITAK_VURDERING"){
                setRowMapper { row -> row.getLong("ID") }
            }
            assertThat(vurderinger).hasSize(2)

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
                    SELECT f.ID, g.BEHANDLING_ID, g.AKTIV, v.PERIODE, v.BEGRUNNELSE, v.HAR_FRITAK
                    FROM MELDEPLIKT_FRITAK_GRUNNLAG g
                    INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
                    INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
                    """.trimIndent()
                ) {
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
                    ),
                )
        }
    }
}
