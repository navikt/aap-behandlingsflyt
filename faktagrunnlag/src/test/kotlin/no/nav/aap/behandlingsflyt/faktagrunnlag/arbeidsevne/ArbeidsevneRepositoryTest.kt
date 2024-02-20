package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbtestdata.ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.Arbeidsevne
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakOgBehandlingService
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class ArbeidsevneRepositoryTest {

    @Test
    fun `Finner ikke arbeidsevne hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val arbeidsevneRepository = ArbeidsevneRepository(connection)
            val arbeidsevneGrunnlag = arbeidsevneRepository.hentHvisEksisterer(behandling.id)
            assertThat(arbeidsevneGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter arbeidsevne`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val arbeidsevneRepository = ArbeidsevneRepository(connection)
            arbeidsevneRepository.lagre(behandling.id, Arbeidsevne("begrunnelse", Prosent(100)))
            val arbeidsevneGrunnlag = arbeidsevneRepository.hentHvisEksisterer(behandling.id)
            assertThat(arbeidsevneGrunnlag?.vurdering).isEqualTo(Arbeidsevne("begrunnelse", Prosent(100)))
        }
    }

    @Test
    fun `Lagrer ikke like arbeidsevner flere ganger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val arbeidsevneRepository = ArbeidsevneRepository(connection)
            arbeidsevneRepository.lagre(behandling.id, Arbeidsevne("en begrunnelse", Prosent(100)))
            arbeidsevneRepository.lagre(behandling.id, Arbeidsevne("annen begrunnelse", Prosent(100)))
            arbeidsevneRepository.lagre(behandling.id, Arbeidsevne("annen begrunnelse", Prosent(100)))

            val opplysninger = connection.queryList(
                """
                    SELECT a.BEGRUNNELSE
                    FROM BEHANDLING b
                    INNER JOIN ARBEIDSEVNE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN ARBEIDSEVNE a ON g.ARBEIDSEVNE_ID = a.ID
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
    fun `Kopierer arbeidsevne fra en behandling til en annen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val arbeidsevneRepository = ArbeidsevneRepository(connection)
            arbeidsevneRepository.lagre(behandling1.id, Arbeidsevne("begrunnelse", Prosent(100)))
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }

            val behandling2 = behandling(connection, sak)
            arbeidsevneRepository.kopier(behandling1.id, behandling2.id)

            val arbeidsevneGrunnlag = arbeidsevneRepository.hentHvisEksisterer(behandling2.id)
            assertThat(arbeidsevneGrunnlag?.vurdering).isEqualTo(Arbeidsevne("begrunnelse", Prosent(100)))
        }
    }

    @Test
    fun `Kopiering av arbeidsevne fra en behandling uten opplysningene skal ikke føre til feil`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val arbeidsevneRepository = ArbeidsevneRepository(connection)
            assertDoesNotThrow {
                arbeidsevneRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Kopierer arbeidsevne fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val arbeidsevneRepository = ArbeidsevneRepository(connection)
            arbeidsevneRepository.lagre(behandling1.id, Arbeidsevne("en begrunnelse", Prosent(100)))
            arbeidsevneRepository.lagre(behandling1.id, Arbeidsevne("annen begrunnelse", Prosent(100)))
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }

            val behandling2 = behandling(connection, sak)
            arbeidsevneRepository.kopier(behandling1.id, behandling2.id)

            val arbeidsevneGrunnlag = arbeidsevneRepository.hentHvisEksisterer(behandling2.id)
            assertThat(arbeidsevneGrunnlag?.vurdering).isEqualTo(Arbeidsevne("annen begrunnelse", Prosent(100)))
        }
    }

    @Test
    fun `Lagrer nye arbeidsevneopplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)
            val arbeidsevneRepository = ArbeidsevneRepository(connection)

            arbeidsevneRepository.lagre(behandling.id, Arbeidsevne("en begrunnelse", Prosent(100)))
            val orginaltGrunnlag = arbeidsevneRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.vurdering).isEqualTo(Arbeidsevne("en begrunnelse", Prosent(100)))

            arbeidsevneRepository.lagre(behandling.id, Arbeidsevne("annen begrunnelse", Prosent(100)))
            val oppdatertGrunnlag = arbeidsevneRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurdering).isEqualTo(Arbeidsevne("annen begrunnelse", Prosent(100)))

            data class Opplysning(
                val aktiv: Boolean,
                val begrunnelse: String,
                val andelNedsattArbeidsevne: Prosent
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT g.AKTIV, a.BEGRUNNELSE, a.ANDEL_AV_NEDSETTELSE
                    FROM BEHANDLING b
                    INNER JOIN ARBEIDSEVNE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN ARBEIDSEVNE a ON g.ARBEIDSEVNE_ID = a.ID
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
                            andelNedsattArbeidsevne = Prosent(row.getInt("ANDEL_AV_NEDSETTELSE"))
                        )
                    }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(
                    Opplysning(aktiv = false, begrunnelse = "en begrunnelse", andelNedsattArbeidsevne = Prosent(100)),
                    Opplysning(aktiv = true, begrunnelse = "annen begrunnelse", andelNedsattArbeidsevne = Prosent(100))
                )
        }
    }

    @Test
    fun `Ved kopiering av arbeidsevneopplysninger fra en avsluttet behandling til en ny skal kun referansen kopieres, ikke hele raden`() {
        InitTestDatabase.dataSource.transaction { connection ->

            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val arbeidsevneRepository = ArbeidsevneRepository(connection)
            arbeidsevneRepository.lagre(behandling1.id, Arbeidsevne("en begrunnelse", Prosent(100)))
            arbeidsevneRepository.lagre(behandling1.id, Arbeidsevne("annen begrunnelse", Prosent(100)))
            connection.execute("UPDATE BEHANDLING SET STATUS = 'AVSLUTTET' WHERE ID = ?") {
                setParams {
                    setLong(1, behandling1.id.toLong())
                }
            }
            val behandling2 = behandling(connection, sak)
            arbeidsevneRepository.kopier(behandling1.id, behandling2.id)

            data class Opplysning(
                val behandlingId: Long,
                val aktiv: Boolean,
                val begrunnelse: String,
                val andelNedsattArbeidsevne: Prosent
            )

            data class Grunnlag(val arbeidsevneId: Long, val opplysning: Opplysning)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID AS BEHANDLING_ID, a.ID AS ARBEIDSEVNE_ID, g.AKTIV, a.BEGRUNNELSE, a.ANDEL_AV_NEDSETTELSE
                    FROM BEHANDLING b
                    INNER JOIN ARBEIDSEVNE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN ARBEIDSEVNE a ON g.ARBEIDSEVNE_ID = a.ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Grunnlag(
                            arbeidsevneId = row.getLong("ARBEIDSEVNE_ID"),
                            opplysning = Opplysning(
                                behandlingId = row.getLong("BEHANDLING_ID"),
                                aktiv = row.getBoolean("AKTIV"),
                                begrunnelse = row.getString("BEGRUNNELSE"),
                                andelNedsattArbeidsevne = Prosent(row.getInt("ANDEL_AV_NEDSETTELSE"))
                            )
                        )
                    }
                }
            assertThat(opplysninger.map(Grunnlag::arbeidsevneId).distinct())
                .hasSize(2)
            assertThat(opplysninger.map(Grunnlag::opplysning))
                .hasSize(3)
                .containsExactly(
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = false,
                        begrunnelse = "en begrunnelse",
                        andelNedsattArbeidsevne = Prosent(100)
                    ),
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = true,
                        begrunnelse = "annen begrunnelse",
                        andelNedsattArbeidsevne = Prosent(100)
                    ),
                    Opplysning(
                        behandlingId = behandling2.id.toLong(),
                        aktiv = true,
                        begrunnelse = "annen begrunnelse",
                        andelNedsattArbeidsevne = Prosent(100)
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

object FakePdlGateway : IdentGateway {
    override fun hentAlleIdenterForPerson(ident: Ident): List<Ident> {
        return listOf(ident)
    }
}