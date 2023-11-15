package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.juni
import no.nav.aap.behandlingsflyt.mai
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.SakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class YrkesskadeRepositoryTest {

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
            connection.execute("TRUNCATE TABLE SAK CASCADE")
        }
    }

    @Test
    fun `Finner ikke yrkesskadeopplysninger hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val yrkesskadeRepository = YrkesskadeRepository(connection)
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling.id)
            assertThat(yrkesskadeGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter yrkesskadeopplysninger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

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
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

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
                connection.queryList("SELECT PERIODE FROM YRKESSKADE_PERIODER") {
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
            val behandling1 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")
            val behandling2 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val yrkesskadeRepository = YrkesskadeRepository(connection)
            yrkesskadeRepository.lagre(
                behandling1.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 juni 2019, 28 juni 2020))))
            )
            yrkesskadeRepository.kopier(
                fraBehandling = behandling1.id,
                tilBehandling = behandling2.id
            )
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling2.id)
            assertThat(yrkesskadeGrunnlag?.yrkesskader).isEqualTo(
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 juni 2019, 28 juni 2020))))
            )
        }
    }

    @Test
    fun `Kopierer yrkesskadeopplysninger fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling1 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")
            val behandling2 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val yrkesskadeRepository = YrkesskadeRepository(connection)
            yrkesskadeRepository.lagre(
                behandling1.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 juni 2019, 28 juni 2020))))
            )
            yrkesskadeRepository.lagre(
                behandling1.id,
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 mai 2019, 28 mai 2020))))
            )
            yrkesskadeRepository.kopier(
                fraBehandling = behandling1.id,
                tilBehandling = behandling2.id
            )
            val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandling2.id)
            assertThat(yrkesskadeGrunnlag?.yrkesskader).isEqualTo(
                Yrkesskader(listOf(Yrkesskade(ref = "ref", periode = Periode(4 mai 2019, 28 mai 2020))))
            )
        }
    }

    @Test
    fun `Lagrer nye opplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
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
                    SELECT BEHANDLING_ID, AKTIV, REFERANSE, PERIODE
                    FROM YRKESSKADE_GRUNNLAG g
                    INNER JOIN YRKESSKADE_PERIODER p ON g.ID = p.GRUNNLAG_ID
                    """.trimIndent()
                ) {
                    setRowMapper { row ->
                        Opplysning(
                            behandlingId = row.getLong("BEHANDLING_ID"),
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
}
