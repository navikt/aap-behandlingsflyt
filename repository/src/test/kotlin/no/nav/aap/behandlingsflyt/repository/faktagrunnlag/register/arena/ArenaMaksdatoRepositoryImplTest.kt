package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.arena

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaVedtakStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.KantIKantMedArena
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.KantIKantUtfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@Tag("db")
class ArenaMaksdatoRepositoryImplTest {

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

    private fun opprettBehandlingId(): BehandlingId = dataSource.transaction { connection ->
        val sak = opprettSak(connection, 1 januar 2026)
        finnEllerOpprettBehandling(connection, sak).id
    }

    @Test
    fun `lagre og hent Arena maksdato`() {
        val behandlingId = opprettBehandlingId()
        val arenaMaksdato = ArenaMaksdato(
            arenaSaksnummer = "123",
            vedtakStatus = ArenaVedtakStatus.LØPENDE,
            maksdato = LocalDate.of(2026, 12, 31),
            vedtakTil = LocalDate.of(2026, 6, 30),
            vedtakId = 42,
            harForlengelseEtter11_12 = true,
        )

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).lagre(behandlingId, arenaMaksdato)
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            ArenaMaksdatoRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag?.arenaMaksdato).isEqualTo(arenaMaksdato)
    }

    @Test
    fun `ny lagring erstatter gammelt grunnlag`() {
        val behandlingId = opprettBehandlingId()
        val gammelt = ArenaMaksdato("123", ArenaVedtakStatus.LØPENDE, LocalDate.of(2026, 12, 31), null, 42, false)
        val nytt = gammelt.copy(vedtakStatus = ArenaVedtakStatus.AVSLUTTET)

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).lagre(behandlingId, gammelt)
            ArenaMaksdatoRepositoryImpl(connection).lagre(behandlingId, nytt)
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            ArenaMaksdatoRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag?.arenaMaksdato).isEqualTo(nytt)
    }

    @Test
    fun `lagre og hente grunnlag uten Arena-sak`() {
        val behandlingId = opprettBehandlingId()

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).lagre(behandlingId, null)
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            ArenaMaksdatoRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag).isNotNull
        assertThat(grunnlag!!.arenaMaksdato).isNull()
    }

    @Test
    fun `kopier kopierer grunnlag til ny behandling`() {
        val fraBehandlingId = opprettBehandlingId()
        val tilBehandlingId = opprettBehandlingId()
        val arenaMaksdato = ArenaMaksdato("123", ArenaVedtakStatus.STANSET, null, LocalDate.of(2026, 6, 30), 42, true)

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).lagre(fraBehandlingId, arenaMaksdato)
            ArenaMaksdatoRepositoryImpl(connection).kopier(fraBehandlingId, tilBehandlingId)
        }

        val kopiert = dataSource.transaction(readOnly = true) { connection ->
            ArenaMaksdatoRepositoryImpl(connection).hentHvisEksisterer(tilBehandlingId)
        }

        assertThat(kopiert?.arenaMaksdato).isEqualTo(arenaMaksdato)
    }

    @Test
    fun `kopier deler Arena-maksdato med forrige behandling og beholder opprettet-tid`() {
        val fraBehandlingId = opprettBehandlingId()
        val tilBehandlingId = opprettBehandlingId()
        val arenaMaksdato = ArenaMaksdato("123", ArenaVedtakStatus.LØPENDE, LocalDate.of(2026, 12, 31), null, 42, false)

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).lagre(fraBehandlingId, arenaMaksdato)
        }
        val opprettetFør = dataSource.transaction(readOnly = true) { connection ->
            hentArenaMaksdatoMedOpprettetTid(connection, fraBehandlingId)
        }

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).kopier(fraBehandlingId, tilBehandlingId)
        }

        val (fra, til) = dataSource.transaction(readOnly = true) { connection ->
            hentArenaMaksdatoMedOpprettetTid(connection, fraBehandlingId) to
                    hentArenaMaksdatoMedOpprettetTid(connection, tilBehandlingId)
        }

        assertThat(til).isNotNull
        assertThat(til).isEqualTo(fra)
        assertThat(til).isEqualTo(opprettetFør)
    }

    @Test
    fun `grunnlag uten Arena-sak lagres uten kobling til Arena-maksdato`() {
        val behandlingId = opprettBehandlingId()

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).lagre(behandlingId, null)
        }

        val manglerKobling = dataSource.transaction(readOnly = true) { connection ->
            connection.queryList(
                "SELECT arena_maksdato_id IS NULL AS mangler FROM arena_maksdato_grunnlag WHERE behandling_id = ? AND aktiv"
            ) {
                setParams { setLong(1, behandlingId.toLong()) }
                setRowMapper { it.getBoolean("mangler") }
            }
        }

        assertThat(manglerKobling).containsExactly(true)
    }

    @Test
    fun `lagring av samme Arena-maksdato gir ikke nytt grunnlag`() {
        val behandlingId = opprettBehandlingId()
        val arenaMaksdato = ArenaMaksdato("123", ArenaVedtakStatus.LØPENDE, LocalDate.of(2026, 12, 31), null, 42, false)

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).lagre(behandlingId, arenaMaksdato)
            ArenaMaksdatoRepositoryImpl(connection).lagre(behandlingId, arenaMaksdato)
        }

        val antallGrunnlag = dataSource.transaction(readOnly = true) { connection ->
            connection.queryFirst("SELECT count(*) AS antall FROM arena_maksdato_grunnlag WHERE behandling_id = ?") {
                setParams { setLong(1, behandlingId.toLong()) }
                setRowMapper { it.getLong("antall") }
            }
        }

        assertThat(antallGrunnlag).isEqualTo(1)
    }

    @Test
    fun `grunnlaget gir id til Arena-maksdato, og id-en er stabil når samme data lagres igjen`() {
        val behandlingId = opprettBehandlingId()
        val arenaMaksdato = ArenaMaksdato("123", ArenaVedtakStatus.LØPENDE, LocalDate.of(2026, 12, 31), null, 42, false)

        val (første, andre) = dataSource.transaction { connection ->
            val repository = ArenaMaksdatoRepositoryImpl(connection)
            repository.lagre(behandlingId, arenaMaksdato)
            val første = repository.hentHvisEksisterer(behandlingId)
            repository.lagre(behandlingId, arenaMaksdato)
            første to repository.hentHvisEksisterer(behandlingId)
        }

        assertThat(første?.arenaMaksdatoId).isNotNull()
        assertThat(andre?.arenaMaksdatoId).isEqualTo(første?.arenaMaksdatoId)
    }

    @Test
    fun `slett beholder Arena-maksdato som en stønadsperiodevurdering peker på`() {
        val behandlingId = opprettBehandlingId()
        val arenaMaksdato = ArenaMaksdato("123", ArenaVedtakStatus.LØPENDE, LocalDate.of(2026, 12, 31), null, 42, false)

        val arenaMaksdatoId = dataSource.transaction { connection ->
            val repository = ArenaMaksdatoRepositoryImpl(connection)
            repository.lagre(behandlingId, arenaMaksdato)
            val id = repository.hentHvisEksisterer(behandlingId)!!.arenaMaksdatoId!!
            StønadsperiodeRepositoryImpl(connection).lagre(
                behandlingId,
                setOf(
                    StønadsperiodeVurdering(
                        referanse = Kravreferanse.ny(),
                        opprettet = Instant.now(),
                        vurdertIBehandling = behandlingId,
                        vurdertAv = SYSTEMBRUKER,
                        begrunnelse = "Test",
                        harHattOrdinærSiste52Uker = true,
                        harGjenværendeKvote = false,
                        relevantKravType = RelevantKravType.NY_STØNADSPERIODE,
                        startDato = LocalDate.of(2027, 1, 1),
                        erKantIKantMedArena = true,
                        kantIKant = KantIKantMedArena(KantIKantUtfall.KANT_I_KANT, id),
                    )
                )
            )
            id
        }

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).slett(behandlingId)
        }

        val gjenværende = dataSource.transaction(readOnly = true) { connection ->
            connection.queryFirst("SELECT count(*) AS antall FROM arena_maksdato WHERE id = ?") {
                setParams { setLong(1, arenaMaksdatoId) }
                setRowMapper { it.getLong("antall") }
            }
        }

        assertThat(gjenværende).isEqualTo(1)
    }

    @Test
    fun `slett beholder Arena-maksdato som deles med en annen behandling`() {
        val fraBehandlingId = opprettBehandlingId()
        val tilBehandlingId = opprettBehandlingId()
        val arenaMaksdato = ArenaMaksdato("123", ArenaVedtakStatus.LØPENDE, LocalDate.of(2026, 12, 31), null, 42, false)

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).lagre(fraBehandlingId, arenaMaksdato)
            ArenaMaksdatoRepositoryImpl(connection).kopier(fraBehandlingId, tilBehandlingId)
            ArenaMaksdatoRepositoryImpl(connection).slett(fraBehandlingId)
        }

        val (slettet, beholdt) = dataSource.transaction(readOnly = true) { connection ->
            ArenaMaksdatoRepositoryImpl(connection).hentHvisEksisterer(fraBehandlingId) to
                    ArenaMaksdatoRepositoryImpl(connection).hentHvisEksisterer(tilBehandlingId)
        }

        assertThat(slettet).isNull()
        assertThat(beholdt?.arenaMaksdato).isEqualTo(arenaMaksdato)
    }

    @Test
    fun `slett fjerner Arena-maksdato som ingen lenger peker på og er idempotent`() {
        val behandlingId = opprettBehandlingId()
        val gammel = ArenaMaksdato("123", ArenaVedtakStatus.LØPENDE, LocalDate.of(2026, 12, 31), null, 42, false)
        val ny = gammel.copy(vedtakStatus = ArenaVedtakStatus.AVSLUTTET)

        val arenaMaksdatoIder = dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).lagre(behandlingId, gammel)
            ArenaMaksdatoRepositoryImpl(connection).lagre(behandlingId, ny)
            connection.queryList(
                "SELECT arena_maksdato_id FROM arena_maksdato_grunnlag WHERE behandling_id = ?"
            ) {
                setParams { setLong(1, behandlingId.toLong()) }
                setRowMapper { it.getLong("arena_maksdato_id") }
            }
        }
        assertThat(arenaMaksdatoIder).hasSize(2)

        dataSource.transaction { connection ->
            ArenaMaksdatoRepositoryImpl(connection).slett(behandlingId)
            ArenaMaksdatoRepositoryImpl(connection).slett(behandlingId)
        }

        val gjenværende = dataSource.transaction(readOnly = true) { connection ->
            connection.queryFirst("SELECT count(*) AS antall FROM arena_maksdato WHERE id = ANY(?::bigint[])") {
                setParams { setLongArray(1, arenaMaksdatoIder) }
                setRowMapper { it.getLong("antall") }
            }
        }

        assertThat(gjenværende).isZero()
    }

    @Test
    fun `rad lagret før vedtak_status fantes gir null for nye felt`() {
        val behandlingId = opprettBehandlingId()

        dataSource.transaction { connection ->
            val arenaMaksdatoId = connection.executeReturnKey(
                """
                    INSERT INTO arena_maksdato (arena_saksnummer, maksdato, vedtak_til, vedtak_id)
                    VALUES ('123', '2026-12-31', '2026-06-30', 42)
                """.trimIndent()
            )
            connection.execute(
                "INSERT INTO arena_maksdato_grunnlag (behandling_id, arena_maksdato_id, opprettet_tid) VALUES (?, ?, now())"
            ) {
                setParams {
                    setLong(1, behandlingId.toLong())
                    setLong(2, arenaMaksdatoId)
                }
            }
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            ArenaMaksdatoRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag?.arenaMaksdato).isEqualTo(
            ArenaMaksdato("123", null, LocalDate.of(2026, 12, 31), LocalDate.of(2026, 6, 30), 42, null)
        )
    }

    private fun hentArenaMaksdatoMedOpprettetTid(
        connection: DBConnection,
        behandlingId: BehandlingId
    ): Pair<Long, LocalDateTime>? = connection.queryFirstOrNull(
        """
            SELECT m.id, m.opprettet_tid
            FROM arena_maksdato_grunnlag g
                     JOIN arena_maksdato m ON m.id = g.arena_maksdato_id
            WHERE g.behandling_id = ? AND g.aktiv
        """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.toLong()) }
        setRowMapper { it.getLong("id") to it.getLocalDateTime("opprettet_tid") }
    }
}
