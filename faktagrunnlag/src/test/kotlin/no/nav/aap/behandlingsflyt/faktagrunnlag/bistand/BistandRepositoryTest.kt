package no.nav.aap.behandlingsflyt.faktagrunnlag.bistand

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
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

class BistandRepositoryTest {

    @Test
    fun `Finner ikke bistand hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val bistandRepository = BistandRepository(connection)
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(bistandGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter bistand`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val bistandRepository = BistandRepository(connection)
            bistandRepository.lagre(behandling.id, BistandVurdering("begrunnelse", false))
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(bistandGrunnlag?.vurdering).isEqualTo(BistandVurdering("begrunnelse", false))
        }
    }

    @Test
    fun `Lagrer ikke like bistand flere ganger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val bistandRepository = BistandRepository(connection)
            bistandRepository.lagre(behandling.id, BistandVurdering("en begrunnelse", false))
            bistandRepository.lagre(behandling.id, BistandVurdering("annen begrunnelse", false))
            bistandRepository.lagre(behandling.id, BistandVurdering("annen begrunnelse", false))

            val opplysninger = connection.queryList(
                """
                    SELECT bi.BEGRUNNELSE
                    FROM BEHANDLING b
                    INNER JOIN BISTAND_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN BISTAND bi ON g.BISTAND_ID = bi.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
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
    fun `Kopierer bistand fra en behandling til en annen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val bistandRepository = BistandRepository(connection)
            bistandRepository.lagre(behandling1.id, BistandVurdering("begrunnelse", false))
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }

            val behandling2 = behandling(connection, sak)
            bistandRepository.kopier(behandling1.id, behandling2.id)

            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling2.id)
            assertThat(bistandGrunnlag?.vurdering).isEqualTo(BistandVurdering("begrunnelse", false))
        }
    }

    @Test
    fun `Kopiering av bistand fra en behandling uten opplysningene skal ikke føre til feil`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val bistandRepository = BistandRepository(connection)
            assertDoesNotThrow {
                bistandRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Kopierer bistand fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val bistandRepository = BistandRepository(connection)
            bistandRepository.lagre(behandling1.id, BistandVurdering("en begrunnelse", false))
            bistandRepository.lagre(behandling1.id, BistandVurdering("annen begrunnelse", false))
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }

            val behandling2 = behandling(connection, sak)
            bistandRepository.kopier(behandling1.id, behandling2.id)

            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling2.id)
            assertThat(bistandGrunnlag?.vurdering).isEqualTo(BistandVurdering("annen begrunnelse", false))
        }
    }

    @Test
    fun `Lagrer nye bistandsopplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)
            val bistandRepository = BistandRepository(connection)

            bistandRepository.lagre(behandling.id, BistandVurdering("en begrunnelse", false))
            val orginaltGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.vurdering).isEqualTo(BistandVurdering("en begrunnelse", false))

            bistandRepository.lagre(behandling.id, BistandVurdering("annen begrunnelse", false))
            val oppdatertGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurdering).isEqualTo(BistandVurdering("annen begrunnelse", false))

            data class Opplysning(
                val aktiv: Boolean,
                val begrunnelse: String,
                val erBehovForBistand: Boolean
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT g.AKTIV, bi.BEGRUNNELSE, bi.ER_BEHOV_FOR_BISTAND
                    FROM BEHANDLING b
                    INNER JOIN BISTAND_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN BISTAND bi ON g.BISTAND_ID = bi.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Opplysning(
                            aktiv = row.getBoolean("AKTIV"),
                            begrunnelse = row.getString("BEGRUNNELSE"),
                            erBehovForBistand = row.getBoolean("ER_BEHOV_FOR_BISTAND")
                        )
                    }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(
                    Opplysning(aktiv = false, begrunnelse = "en begrunnelse", erBehovForBistand = false),
                    Opplysning(aktiv = true, begrunnelse = "annen begrunnelse", erBehovForBistand = false)
                )
        }
    }

    @Test
    fun `Ved kopiering av bistandsopplysninger fra en avsluttet behandling til en ny skal kun referansen kopieres, ikke hele raden`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val bistandRepository = BistandRepository(connection)
            bistandRepository.lagre(behandling1.id, BistandVurdering("en begrunnelse", false))
            bistandRepository.lagre(behandling1.id, BistandVurdering("annen begrunnelse", false))
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = behandling(connection, sak)
            bistandRepository.kopier(behandling1.id, behandling2.id)

            data class Opplysning(
                val behandlingId: Long,
                val aktiv: Boolean,
                val begrunnelse: String,
                val erBehovForBistand: Boolean
            )

            data class Grunnlag(val bistandId: Long, val opplysning: Opplysning)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID AS BEHANDLING_ID, bi.ID AS BISTAND_ID, g.AKTIV, bi.BEGRUNNELSE, bi.ER_BEHOV_FOR_BISTAND
                    FROM BEHANDLING b
                    INNER JOIN BISTAND_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN BISTAND bi ON g.BISTAND_ID = bi.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Grunnlag(
                            bistandId = row.getLong("BISTAND_ID"),
                            opplysning = Opplysning(
                                behandlingId = row.getLong("BEHANDLING_ID"),
                                aktiv = row.getBoolean("AKTIV"),
                                begrunnelse = row.getString("BEGRUNNELSE"),
                                erBehovForBistand = row.getBoolean("ER_BEHOV_FOR_BISTAND")
                            )
                        )
                    }
                }
            assertThat(opplysninger.map(Grunnlag::bistandId).distinct())
                .hasSize(2)
            assertThat(opplysninger.map(Grunnlag::opplysning))
                .hasSize(3)
                .containsExactly(
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = false,
                        begrunnelse = "en begrunnelse",
                        erBehovForBistand = false
                    ),
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = true,
                        begrunnelse = "annen begrunnelse",
                        erBehovForBistand = false
                    ),
                    Opplysning(
                        behandlingId = behandling2.id.toLong(),
                        aktiv = true,
                        begrunnelse = "annen begrunnelse",
                        erBehovForBistand = false
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
