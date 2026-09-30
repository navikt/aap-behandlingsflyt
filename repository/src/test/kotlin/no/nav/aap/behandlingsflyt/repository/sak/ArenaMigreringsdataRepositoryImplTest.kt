package no.nav.aap.behandlingsflyt.repository.sak

import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Instant

class ArenaMigreringsdataRepositoryImplTest {
    companion object {
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

    private data class Testdata(val begrunnelse: String, val koder: List<String>)

    @Test
    fun `lagre og hent aktiv rad`() {
        val behandlingId = opprettBehandling()
        val hentetTidspunkt = Instant.parse("2024-06-15T12:30:00Z")

        dataSource.transaction {
            ArenaMigreringsdataRepositoryImpl(it).lagre(
                behandlingId, StegType.AVKLAR_SYKDOM, Testdata("Begrunnelse", listOf("A", "B")), hentetTidspunkt
            )
        }

        val hentet = dataSource.transaction {
            ArenaMigreringsdataRepositoryImpl(it).hentAktivHvisEksisterer(behandlingId, StegType.AVKLAR_SYKDOM)
        }

        assertThat(hentet).isNotNull
        assertThat(hentet!!.behandlingId).isEqualTo(behandlingId)
        assertThat(hentet.steg).isEqualTo(StegType.AVKLAR_SYKDOM)
        assertThat(hentet.hentetTidspunkt).isEqualTo(hentetTidspunkt)
        assertThat(hentet.data).contains("\"begrunnelse\"", "Begrunnelse", "\"A\"", "\"B\"")
    }

    @Test
    fun `lik data lagres ikke på nytt og beholder første hentet_tidspunkt`() {
        val behandlingId = opprettBehandling()
        val først = Instant.parse("2024-06-15T12:00:00Z")
        val senere = først.plusSeconds(3600)

        dataSource.transaction {
            val repo = ArenaMigreringsdataRepositoryImpl(it)
            repo.lagre(behandlingId, StegType.AVKLAR_SYKDOM, Testdata("Lik", listOf("A")), først)
            repo.lagre(behandlingId, StegType.AVKLAR_SYKDOM, Testdata("Lik", listOf("A")), senere)
        }

        val (hentet, antall) = dataSource.transaction { connection ->
            val hentet = ArenaMigreringsdataRepositoryImpl(connection)
                .hentAktivHvisEksisterer(behandlingId, StegType.AVKLAR_SYKDOM)
            hentet to antallRader(connection, behandlingId).first
        }

        assertThat(antall).isEqualTo(1)
        assertThat(hentet!!.hentetTidspunkt).isEqualTo(først)
    }

    @Test
    fun `ny lagring deaktiverer forrige rad for samme behandling og steg`() {
        val behandlingId = opprettBehandling()

        dataSource.transaction {
            val repo = ArenaMigreringsdataRepositoryImpl(it)
            repo.lagre(behandlingId, StegType.AVKLAR_SYKDOM, Testdata("Første", emptyList()), Instant.now())
            repo.lagre(behandlingId, StegType.AVKLAR_SYKDOM, Testdata("Andre", emptyList()), Instant.now())
        }

        val (hentet, antallRader, antallAktive) = dataSource.transaction { connection ->
            val hentet = ArenaMigreringsdataRepositoryImpl(connection)
                .hentAktivHvisEksisterer(behandlingId, StegType.AVKLAR_SYKDOM)
            val antall = antallRader(connection, behandlingId)
            Triple(hentet, antall.first, antall.second)
        }

        assertThat(hentet!!.data).contains("Andre")
        assertThat(antallRader).isEqualTo(2)
        assertThat(antallAktive).isEqualTo(1)
    }

    @Test
    fun `ulike steg på samme behandling er uavhengige`() {
        val behandlingId = opprettBehandling()

        dataSource.transaction {
            val repo = ArenaMigreringsdataRepositoryImpl(it)
            repo.lagre(behandlingId, StegType.AVKLAR_SYKDOM, Testdata("Sykdom", emptyList()), Instant.now())
            repo.lagre(behandlingId, StegType.VURDER_BISTANDSBEHOV, Testdata("Bistand", emptyList()), Instant.now())
        }

        dataSource.transaction {
            val repo = ArenaMigreringsdataRepositoryImpl(it)
            assertThat(repo.hentAktivHvisEksisterer(behandlingId, StegType.AVKLAR_SYKDOM)!!.data).contains("Sykdom")
            assertThat(repo.hentAktivHvisEksisterer(behandlingId, StegType.VURDER_BISTANDSBEHOV)!!.data).contains("Bistand")
        }
    }

    @Test
    fun `hentAktivHvisEksisterer returnerer null når ingenting er lagret`() {
        val behandlingId = opprettBehandling()

        val hentet = dataSource.transaction {
            ArenaMigreringsdataRepositoryImpl(it).hentAktivHvisEksisterer(behandlingId, StegType.AVKLAR_SYKDOM)
        }

        assertThat(hentet).isNull()
    }

    private fun antallRader(connection: DBConnection, behandlingId: BehandlingId): Pair<Long, Long> =
        connection.queryFirst(
            "SELECT COUNT(*) AS antall, COUNT(*) FILTER (WHERE aktiv) AS aktive FROM arena_migreringsdata WHERE behandling_id = ?"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row -> row.getLong("antall") to row.getLong("aktive") }
        }

    private fun opprettBehandling(): BehandlingId = dataSource.transaction { connection ->
        val sak = opprettSak(connection, LocalDate.now())
        finnEllerOpprettBehandling(connection, sak).id
    }
}
