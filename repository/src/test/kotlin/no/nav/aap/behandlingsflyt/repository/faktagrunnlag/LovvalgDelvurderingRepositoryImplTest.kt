package no.nav.aap.behandlingsflyt.repository.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.ArbeidInntektINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.Indikasjon
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.KanBehandlesAutomatiskVurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.Kilde
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.TilhørighetVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.LovvalgDelvurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class LovvalgDelvurderingRepositoryImplTest {
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

    val startDato = 1 januar 2026

    private val vurdering = KanBehandlesAutomatiskVurdering(
        kanBehandlesAutomatisk = true,
        tilhørighetVurdering = listOf(
            TilhørighetVurdering(
                kilde = listOf(Kilde.A_INNTEKT),
                indikasjon = Indikasjon.I_NORGE,
                opplysning = "Arbeid og inntekt i Norge",
                resultat = true,
                vurdertPeriode = "Inneværende og to forrige måneder",
                arbeidInntektINorgeGrunnlag = listOf(
                    ArbeidInntektINorgeGrunnlag(
                        "123456789", "Testbedrift", 1234.5, Periode(1 januar 2026, 31 januar 2026)
                    )
                )
            )
        )
    )

    @Test
    fun `lagrer hele vurderingen og skriver ikke ny historikk ved uendret resultat`() {
        dataSource.transaction { connection ->
            val behandlingId = opprettBehandling(connection)
            val repository = LovvalgDelvurderingRepositoryImpl(connection)
            assertThat(repository.hentHvisEksisterer(behandlingId)).isNull()

            repository.lagre(behandlingId, vurdering, startDato)
            repository.lagre(behandlingId, vurdering.copy(), startDato)

            val grunnlag = requireNotNull(repository.hentHvisEksisterer(behandlingId))
            assertThat(grunnlag.vurdering()).isEqualTo(vurdering)
            assertThat(grunnlag.relevantDato).isEqualTo(startDato)
            assertThat(grunnlag.delvurderingId).isPositive()
            assertThat(repository.hentBasertPåDelvurderingId(grunnlag.delvurderingId)).isEqualTo(grunnlag)
            assertThat(historikk(connection, behandlingId)).containsExactly(true)
        }
    }

    @Test
    fun `endret relevant dato lagres som ny historikk`() {
        dataSource.transaction { connection ->
            val behandlingId = opprettBehandling(connection)
            val repository = LovvalgDelvurderingRepositoryImpl(connection)
            val nyDato = startDato.plusDays(1)

            repository.lagre(behandlingId, vurdering, startDato)
            repository.lagre(behandlingId, vurdering, nyDato)

            assertThat(repository.hentHvisEksisterer(behandlingId)?.relevantDato).isEqualTo(nyDato)
            assertThat(historikk(connection, behandlingId)).containsExactly(false, true)
        }
    }

    @Test
    fun `endret delvurdering med samme hovedresultat deaktiverer gammel rad`() {
        dataSource.transaction { connection ->
            val behandlingId = opprettBehandling(connection)
            val repository = LovvalgDelvurderingRepositoryImpl(connection)
            val endret = vurdering.copy(
                tilhørighetVurdering = vurdering.tilhørighetVurdering.map {
                    it.copy(arbeidInntektINorgeGrunnlag = it.arbeidInntektINorgeGrunnlag?.map { inntekt ->
                        inntekt.copy(beloep = 2000.0)
                    })
                }
            )
            repository.lagre(behandlingId, vurdering, startDato)
            val opprinnelig = requireNotNull(repository.hentHvisEksisterer(behandlingId))
            repository.lagre(behandlingId, endret, startDato)
            repository.lagre(behandlingId, endret, startDato)

            assertThat(repository.hentHvisEksisterer(behandlingId)?.vurdering()).isEqualTo(endret)
            assertThat(repository.hentBasertPåDelvurderingId(opprinnelig.delvurderingId)).isEqualTo(opprinnelig)
            assertThat(historikk(connection, behandlingId)).containsExactly(false, true)
            val gammelVurdering = connection.queryFirst(
                """
                SELECT v.vurdering FROM LOVVALG_DELVURDERING_GRUNNLAG g
                JOIN LOVVALG_DELVURDERING v ON v.vurderinger_id = g.vurderinger_id
                WHERE g.behandling_id = ? AND NOT g.aktiv
                """.trimIndent()
            ) {
                setParams { setLong(1, behandlingId.toLong()) }
                setRowMapper { it.getString("vurdering") }
            }
            assertThat(gammelVurdering).contains("1234.5")
        }
    }

    @Test
    fun `endret hovedresultat lagres og vurderinger er adskilt per behandling`() {
        dataSource.transaction { connection ->
            val første = opprettBehandling(connection)
            val andre = opprettBehandling(connection)
            val repository = LovvalgDelvurderingRepositoryImpl(connection)
            val endret = vurdering.copy(kanBehandlesAutomatisk = false)

            repository.lagre(første, vurdering, startDato)
            repository.lagre(andre, vurdering, startDato)
            repository.lagre(første, endret, startDato)

            assertThat(repository.hentHvisEksisterer(første)?.vurdering()).isEqualTo(endret)
            assertThat(repository.hentHvisEksisterer(andre)?.vurdering()).isEqualTo(vurdering)
            assertThat(historikk(connection, andre)).containsExactly(true)

        }
    }

    private fun opprettBehandling(connection: DBConnection): BehandlingId {
        return finnEllerOpprettBehandling(connection, opprettSak(connection, startDato)).id
    }

    @Test
    fun `kopiering deler vurderingssett og beholder opprinnelig behandling ved uendret vurdering`() {
        dataSource.transaction { connection ->
            val fra = opprettBehandling(connection)
            val til = opprettBehandling(connection)
            val repository = LovvalgDelvurderingRepositoryImpl(connection)
            repository.lagre(fra, vurdering, startDato)
            repository.kopier(fra, til)
            repository.lagre(til, vurdering, startDato)

            val kopiert = requireNotNull(repository.hentHvisEksisterer(til))
            assertThat(kopiert.vurdering()).isEqualTo(vurdering)
            assertThat(kopiert.delvurderingId)
                .isEqualTo(repository.hentHvisEksisterer(fra)?.delvurderingId)
            assertThat(vurderingerId(connection, til)).isEqualTo(vurderingerId(connection, fra))
            assertThat(historikk(connection, til)).containsExactly(true)
            assertThat(innhold(connection, vurderingerId(connection, til))).containsExactly(fra)
        }
    }

    @Test
    fun `endring etter kopiering oppretter eget sett uten å endre kildebehandlingen`() {
        dataSource.transaction { connection ->
            val fra = opprettBehandling(connection)
            val til = opprettBehandling(connection)
            val repository = LovvalgDelvurderingRepositoryImpl(connection)
            repository.lagre(fra, vurdering, startDato)
            val opprinneligSett = vurderingerId(connection, fra)
            repository.kopier(fra, til)
            val endret = vurdering.copy(kanBehandlesAutomatisk = false)
            repository.lagre(til, endret, startDato)

            assertThat(repository.hentHvisEksisterer(fra)?.vurdering()).isEqualTo(vurdering)
            assertThat(repository.hentHvisEksisterer(til)?.vurdering()).isEqualTo(endret)
            assertThat(repository.hentHvisEksisterer(til)?.delvurderingId)
                .isNotEqualTo(repository.hentHvisEksisterer(fra)?.delvurderingId)
            assertThat(historikk(connection, fra)).containsExactly(true)
            assertThat(historikk(connection, til)).containsExactly(false, true)
            assertThat(vurderingerId(connection, til)).isNotEqualTo(opprinneligSett)
            assertThat(innhold(connection, opprinneligSett)).containsExactly(fra)
            assertThat(innhold(connection, vurderingerId(connection, til))).containsExactly(til)
        }
    }

    @Test
    fun `slette alt for en behandling`() {
        dataSource.transaction { connection ->
            val behandling = opprettBehandling(connection)
            val repository = LovvalgDelvurderingRepositoryImpl(connection)
            repository.lagre(behandling, vurdering, startDato)
            val vurderingerId = vurderingerId(connection, behandling)

            repository.slett(behandling)
            assertThat(innhold(connection, vurderingerId)).isEmpty()

            val vurderingerIdEtterSletting = connection.queryFirstOrNull("SELECT id FROM LOVVALG_DELVURDERINGER WHERE id = ?") {
                setParams { setLong(1, vurderingerId) }
                setRowMapper { it.getLong("id") }
            }
            assertThat(vurderingerIdEtterSletting).isNull()
        }
    }

    @Test
    fun `oppslag med ukjent delvurderingId returnerer null`() {
        dataSource.transaction { connection ->
            val repository = LovvalgDelvurderingRepositoryImpl(connection)
            assertThat(repository.hentBasertPåDelvurderingId(-1L)).isNull()
        }
    }

    @Test
    fun `kopiering uten kildegrunnlag oppretter ikke grunnlag`() {
        dataSource.transaction { connection ->
            val repository = LovvalgDelvurderingRepositoryImpl(connection)
            val fra = opprettBehandling(connection)
            val til = opprettBehandling(connection)
            repository.kopier(fra, til)
            assertThat(repository.hentHvisEksisterer(til)).isNull()
            assertThat(historikk(connection, til)).isEmpty()
        }
    }

    private fun vurderingerId(connection: DBConnection, behandlingId: BehandlingId): Long =
        connection.queryFirst(
            "SELECT vurderinger_id FROM LOVVALG_DELVURDERING_GRUNNLAG WHERE behandling_id = ? AND aktiv"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getLong("vurderinger_id") }
        }

    private fun innhold(connection: DBConnection, vurderingerId: Long): List<BehandlingId> =
        connection.queryList(
            "SELECT vurdert_i_behandling FROM LOVVALG_DELVURDERING WHERE vurderinger_id = ?"
        ) {
            setParams { setLong(1, vurderingerId) }
            setRowMapper { BehandlingId(it.getLong("vurdert_i_behandling")) }
        }

    private fun historikk(connection: DBConnection, behandlingId: BehandlingId): List<Boolean> =
        connection.queryList(
            "SELECT aktiv FROM LOVVALG_DELVURDERING_GRUNNLAG WHERE behandling_id = ? ORDER BY id"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getBoolean("aktiv") }
        }
}
