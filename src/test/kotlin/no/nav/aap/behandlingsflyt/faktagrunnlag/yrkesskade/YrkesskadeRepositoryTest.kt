package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.juni
import no.nav.aap.behandlingsflyt.mai
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.SakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class YrkesskadeRepositoryTest {

    @Test
    fun `Finner ikke yrkesskadeopplysninger hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = behandling(connection, sak(connection))

            val yrkesskadeRepository = YrkesskadeRepository(connection)
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(yrkesskadeGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter yrkesskadeopplysninger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = behandling(connection, sak(connection))

            val yrkesskadeRepository = YrkesskadeRepository(connection)
            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 juni 2019, 28 juni 2020))))
            )
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(yrkesskadeGrunnlag?.yrkesskader).isEqualTo(
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 juni 2019, 28 juni 2020))))
            )
        }
    }

    @Test
    fun `Lagrer ikke like opplysninger flere ganger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)

            val yrkesskadeRepository = YrkesskadeRepository(connection)
            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 juni 2019, 28 juni 2020))))
            )
            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 mai 2019, 28 mai 2020))))
            )
            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 mai 2019, 28 mai 2020))))
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID, g.AKTIV, p.REFERANSE, p.PERIODE
                    FROM BEHANDLING b
                    INNER JOIN YRKESSKADE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN YRKESSKADE_PERIODER p ON g.ID = p.GRUNNLAG_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row -> row.getPeriode("PERIODE") }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(Periode(4 juni 2019, 28 juni 2020), Periode(4 mai 2019, 28 mai 2020))
        }
    }

    @Test
    fun `Kopierer yrkesskadeopplysninger fra en behandling til en annen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val yrkesskadeRepository = YrkesskadeRepository(connection)
            yrkesskadeRepository.lagre(
                behandling1.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 juni 2019, 28 juni 2020))))
            )
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")
            val behandling2 = behandling(connection, sak)

            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling2.id)
            assertThat(yrkesskadeGrunnlag?.yrkesskader).isEqualTo(
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 juni 2019, 28 juni 2020))))
            )
        }
    }

    @Test
    fun `Kopierer yrkesskadeopplysninger fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = behandling(connection, sak)
            val yrkesskadeRepository = YrkesskadeRepository(connection)
            yrkesskadeRepository.lagre(
                behandling1.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 juni 2019, 28 juni 2020))))
            )
            yrkesskadeRepository.lagre(
                behandling1.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 mai 2019, 28 mai 2020))))
            )
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")
            val behandling2 = behandling(connection, sak)

            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling2.id)
            assertThat(yrkesskadeGrunnlag?.yrkesskader).isEqualTo(
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 mai 2019, 28 mai 2020))))
            )
        }
    }

    @Test
    fun `Lagrer nye opplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = behandling(connection, sak)
            val yrkesskadeRepository = YrkesskadeRepository(connection)

            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 juni 2019, 28 juni 2020))))
            )
            val orginaltGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.yrkesskader).isEqualTo(
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 juni 2019, 28 juni 2020))))
            )

            yrkesskadeRepository.lagre(
                behandling.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 mai 2019, 28 mai 2020))))
            )
            val oppdatertGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.yrkesskader).isEqualTo(
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 mai 2019, 28 mai 2020))))
            )

            data class Opplysning(val behandlingId: Long, val aktiv: Boolean, val ref: String, val periode: Periode)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID, g.AKTIV, p.REFERANSE, p.PERIODE
                    FROM BEHANDLING b
                    INNER JOIN YRKESSKADE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN YRKESSKADE_PERIODER p ON g.ID = p.GRUNNLAG_ID
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
                            periode = row.getPeriode("PERIODE")
                        )
                    }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(
                    Opplysning(behandling.id.toLong(), false, "ref", Periode(4 juni 2019, 28 juni 2020)),
                    Opplysning(behandling.id.toLong(), true, "ref", Periode(4 mai 2019, 28 mai 2020))
                )
        }
    }

    private companion object {
        private val identTeller = AtomicInteger(0)
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        private fun ident(): Ident {
            return Ident(identTeller.getAndAdd(1).toString())
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return SakRepository(connection).finnEllerOpprett(
            person = PersonRepository(connection).finnEllerOpprett(ident()),
            periode = periode
        )
    }

    private fun behandling(connection: DBConnection, sak: Sak): Behandling {
        val behandling = BehandlingRepository(connection).finnSisteBehandlingFor(sak.id)
        if (behandling == null || behandling.status().erAvsluttet()) {
            return BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
        }
        return behandling
    }
}
