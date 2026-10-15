package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.tellDager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class BackfillEtableringVurderingerTest {
    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setUp() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    private val utviklingsperiode1 = Periode(1 januar 2025, 10 januar 2025)
    private val utviklingsperiode2 = Periode(11 januar 2025, 20 januar 2025)
    private val oppstartsperiode = Periode(1 februar 2025, 10 februar 2025)

    @Test
    fun `splitter hver periode i egen vurdering med fase`() {
        //                   |des      |jan      |feb      |mar
        // fom               F
        // utvikling 1                 UUUU
        // utvikling 2                    UUUU
        // oppstart                              OOOO
        // => utvikling 1              UUUU
        // => utvikling 2                 UUUU
        // => oppstart                           OOOO
        // F=fom, U=utvikling, O=oppstart
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 1 desember 2024,
            utviklingsPerioder = listOf(utviklingsperiode1, utviklingsperiode2),
            oppstartsPerioder = listOf(oppstartsperiode),
        )
        lagre(behandlingId, legacy)

        migrer(behandlingId)

        assertThat(hentVurderinger(behandlingId)).containsExactlyInAnyOrder(
            legacy.somUtvikling(utviklingsperiode1),
            legacy.somUtvikling(utviklingsperiode2),
            legacy.somOppstart(oppstartsperiode),
        )
    }

    @Test
    fun `migrering er idempotent`() {
        //                   |des      |jan      |feb      |mar
        // fom                         F
        // utvikling 1                 UUUU
        // utvikling 2                    UUUU
        // oppstart                              OOOO
        // => utvikling 1              UUUU
        // => utvikling 2                 UUUU
        // => oppstart                           OOOO
        // F=fom, U=utvikling, O=oppstart
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 1 januar 2025,
            utviklingsPerioder = listOf(utviklingsperiode1, utviklingsperiode2),
            oppstartsPerioder = listOf(oppstartsperiode),
        )
        lagre(behandlingId, legacy)
        val idererFørMigrering = hentVurderingIder(behandlingId)

        migrer(behandlingId)
        val vurderingerEtterFørsteKjøring = hentVurderinger(behandlingId)
        val idererEtterFørsteKjøring = hentVurderingIder(behandlingId)

        migrer(behandlingId)
        migrer(behandlingId)

        assertThat(idererEtterFørsteKjøring).hasSize(3).doesNotContainAnyElementsOf(idererFørMigrering)
        assertThat(hentVurderinger(behandlingId)).containsExactlyInAnyOrderElementsOf(vurderingerEtterFørsteKjøring)
        assertThat(hentVurderingIder(behandlingId)).containsExactlyInAnyOrderElementsOf(idererEtterFørsteKjøring)
    }

    @Test
    fun `bruker periodens fom selv om perioden starter før vurderingens fom`() {
        //                   |des      |jan      |feb      |mar
        // fom                          F
        // utvikling 1                 UUUU
        // utvikling 2                    UUUU
        // => utvikling 1              UUUU
        // => utvikling 2                 UUUU
        // F=fom, U=utvikling, O=oppstart
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 5 januar 2025,
            utviklingsPerioder = listOf(utviklingsperiode1, utviklingsperiode2),
        )
        lagre(behandlingId, legacy)

        migrer(behandlingId)

        assertThat(hentVurderinger(behandlingId)).containsExactlyInAnyOrder(
            legacy.somUtvikling(utviklingsperiode1),
            legacy.somUtvikling(utviklingsperiode2),
        )
    }

    @Test
    fun `bruker periodens fom selv om vurderingens fom er før andre periode`() {
        //                   |des      |jan      |feb      |mar
        // fom                         F
        // utvikling 1                 UUUU
        // utvikling 2                    UUUU
        // oppstart                              OOOO
        // => utvikling 1              UUUU
        // => utvikling 2                 UUUU
        // => oppstart                           OOOO
        // F=fom, U=utvikling, O=oppstart
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 1 januar 2025,
            utviklingsPerioder = listOf(utviklingsperiode1, utviklingsperiode2),
            oppstartsPerioder = listOf(oppstartsperiode),
        )
        lagre(behandlingId, legacy)

        migrer(behandlingId)

        assertThat(hentVurderinger(behandlingId)).containsExactlyInAnyOrder(
            legacy.somUtvikling(utviklingsperiode1),
            legacy.somUtvikling(utviklingsperiode2),
            legacy.somOppstart(oppstartsperiode),
        )
    }

    @Test
    fun `bruker periodens fom og tom selv om vurderingen har tom som ikke samsvarer med periodene`() {
        //                   |des      |jan      |feb      |mar
        // fom-tom           FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
        // oppstart                              OOOO
        // => oppstart                           OOOO
        // F=fom, U=utvikling, O=oppstart
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 1 desember 2024,
            tom = 31 mars 2025,
            oppstartsPerioder = listOf(oppstartsperiode),
        )
        lagre(behandlingId, legacy)

        migrer(behandlingId)

        assertThat(hentVurderinger(behandlingId)).containsExactly(legacy.somOppstart(oppstartsperiode))
    }

    @Test
    fun `lar vurdering uten perioder være uendret`() {
        //                   |des      |jan      |feb      |mar
        // fom                         F
        // F=fom, U=utvikling, O=oppstart
        val behandlingId = opprettBehandling()
        val utenPerioder = vurdering(behandlingId, fom = 1 januar 2025)
        lagre(behandlingId, utenPerioder)
        val idererFør = hentVurderingIder(behandlingId)

        migrer(behandlingId)

        assertThat(hentVurderinger(behandlingId)).containsExactly(utenPerioder)
        assertThat(hentVurderingIder(behandlingId)).isEqualTo(idererFør)
    }

    @Test
    fun `rører ikke vurderinger som allerede har fase`() {
        //                   |des      |jan      |feb      |mar
        // migrert utvikling           UUUU
        // legacy fom                            F
        // legacy oppstart                       OOOO
        // => oppstart                           OOOO
        // F=fom, U=utvikling, O=oppstart
        val behandlingId = opprettBehandling()
        val alleredeMigrert = vurdering(
            behandlingId,
            fom = utviklingsperiode1.fom,
            tom = utviklingsperiode1.tom,
            utviklingsPerioder = listOf(utviklingsperiode1),
            fase = EtableringFase.UTVIKLING,
        )
        val legacy = vurdering(
            behandlingId,
            fom = 1 februar 2025,
            oppstartsPerioder = listOf(oppstartsperiode),
            virksomhetNavn = "Legacy",
        )
        lagre(behandlingId, alleredeMigrert, legacy)
        val idTilAlleredeMigrert = hentVurderingIder(behandlingId).first()

        migrer(behandlingId)

        assertThat(hentVurderinger(behandlingId)).containsExactlyInAnyOrder(
            alleredeMigrert,
            legacy.somOppstart(oppstartsperiode),
        )
        assertThat(hentVurderingIder(behandlingId)).contains(idTilAlleredeMigrert)
    }

    @Test
    fun `total varighet er uendret for en behandling`() {
        //                   |des      |jan      |feb      |mar
        // fom                         F
        // utvikling 1                 UUUU
        // utvikling 2                    UUUU
        // oppstart                              OOOO
        // => utvikling 1              UUUU
        // => utvikling 2                 UUUU
        // => oppstart                           OOOO
        // F=fom, U=utvikling, O=oppstart
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 1 januar 2025,
            utviklingsPerioder = listOf(utviklingsperiode1, utviklingsperiode2),
            oppstartsPerioder = listOf(oppstartsperiode),
        )

        assertThat(varighet(migrert(legacy))).isEqualTo(varighet(listOf(legacy)))
    }

    @Test
    fun `total varighet er uendret når senere vurdering overstyrer deler av en eldre`() {
        //                   |des      |jan      |feb      |mar
        // eldre fom                   F
        // eldre utvikling 1           UUUU
        // eldre utvikling 2                               UUUU
        // nyere fom                             F
        // F=fom, U=utvikling, O=oppstart
        val førsteBehandling = opprettBehandling()
        val andreBehandling = opprettBehandling()
        val eldre = vurdering(
            førsteBehandling,
            fom = 1 januar 2025,
            utviklingsPerioder = listOf(utviklingsperiode1, Periode(1 mars 2025, 10 mars 2025)),
        )
        val nyere = vurdering(andreBehandling, fom = 1 februar 2025, opprettet = Instant.parse("2025-02-01T00:00:00Z"))

        assertThat(varighet(migrert(eldre) + nyere)).isEqualTo(varighet(listOf(eldre, nyere)))
    }

    @Test
    fun `total varighet og evaluering er uendret for liste med både oppfylte og ikke-oppfylte vurderinger`() {
        //                   |des      |jan      |feb      |mar
        // oppfylt fom                 F
        // oppfylt utvikling 1          UUUU
        // oppfylt utvikling 2             UUUU
        // ikke oppfylt fom                      F
        // ikke oppfylt oppstart                    OOOO
        // uten perioder fom                               F
        // F=fom, U=utvikling, O=oppstart
        val behandlingId = opprettBehandling()
        val oppfylt = vurdering(
            behandlingId,
            fom = 1 januar 2025,
            utviklingsPerioder = listOf(utviklingsperiode1, utviklingsperiode2),
            virksomhetErNy = true,
        )
        val ikkeOppfylt = vurdering(
            behandlingId,
            fom = 1 februar 2025,
            oppstartsPerioder = listOf(oppstartsperiode),
            virksomhetErNy = false,
        )
        val utenPerioder = vurdering(behandlingId, fom = 1 mars 2025, virksomhetErNy = false)
        val før = listOf(oppfylt, ikkeOppfylt, utenPerioder)
        val etter = før.flatMap { migrert(it) }

        assertThat(evalueringer(før)).containsExactly(true, false, false)
        assertThat(varighet(etter)).isEqualTo(varighet(før))
        assertThat(etter.map { it.evaluerVirksomhetVurdering() })
            .containsExactlyInAnyOrder(true, true, false, false)
    }

    private fun evalueringer(vurderinger: List<EtableringEgenVirksomhetVurdering>) =
        vurderinger.map { it.evaluerVirksomhetVurdering() }

    private fun migrert(vurdering: EtableringEgenVirksomhetVurdering) =
        BackfillEtableringVurderinger(dataSource).oppdaterVurdering(1L to vurdering).map { it.second }

    private fun varighet(vurderinger: List<EtableringEgenVirksomhetVurdering>): Pair<Int, Int> {
        val gjeldende = vurderinger.gjeldendeVurderinger().verdier().toSet()
        return gjeldende.tellDager { it.utviklingsPerioder } to gjeldende.tellDager { it.oppstartsPerioder }
    }

    private fun EtableringEgenVirksomhetVurdering.somUtvikling(periode: Periode) = copy(
        utviklingsPerioder = listOf(periode),
        oppstartsPerioder = emptyList(),
        fase = EtableringFase.UTVIKLING,
        fom = periode.fom,
        tom = periode.tom,
    )

    private fun EtableringEgenVirksomhetVurdering.somOppstart(periode: Periode) = copy(
        utviklingsPerioder = emptyList(),
        oppstartsPerioder = listOf(periode),
        fase = EtableringFase.OPPSTART,
        fom = periode.fom,
        tom = periode.tom,
    )

    private fun vurdering(
        behandlingId: BehandlingId,
        fom: LocalDate,
        tom: LocalDate? = null,
        utviklingsPerioder: List<Periode> = emptyList(),
        oppstartsPerioder: List<Periode> = emptyList(),
        fase: EtableringFase? = null,
        virksomhetNavn: String = "Trout&Tractor",
        opprettet: Instant = Instant.parse("2025-01-01T00:00:00Z"),
        virksomhetErNy: Boolean = false,
    ) = EtableringEgenVirksomhetVurdering(
        begrunnelse = "Historisk vurdering",
        virksomhetNavn = virksomhetNavn,
        orgNr = "org",
        foreliggerFagligVurdering = true,
        virksomhetErNy = virksomhetErNy,
        brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT_MED_FLER,
        kanFøreTilSelvforsørget = true,
        utviklingsPerioder = utviklingsPerioder,
        oppstartsPerioder = oppstartsPerioder,
        vurdertAv = Bruker("Z123456"),
        opprettet = opprettet,
        vurdertIBehandling = behandlingId,
        fom = fom,
        tom = tom,
        fase = fase,
        erRegistrertINødvendigeOffentligeRegister = null,
        jobberBrukerAktivMedVirksomheten = null,
    )

    private fun opprettBehandling(): BehandlingId = dataSource.transaction { connection ->
        finnEllerOpprettBehandling(connection, sak(connection)).id
    }

    private fun lagre(behandlingId: BehandlingId, vararg vurderinger: EtableringEgenVirksomhetVurdering) {
        dataSource.transaction { connection ->
            postgresRepositoryRegistry.provider(connection)
                .provide<EtableringEgenVirksomhetRepository>()
                .lagre(behandlingId, vurderinger.toList())
        }
    }

    private fun migrer(behandlingId: BehandlingId) {
        val backfill = BackfillEtableringVurderinger(dataSource)
        dataSource.transaction { connection ->
            backfill.migrerBehandling(connection, behandlingId)
        }
    }

    private fun hentVurderinger(behandlingId: BehandlingId): List<EtableringEgenVirksomhetVurdering> =
        dataSource.transaction { connection ->
            postgresRepositoryRegistry.provider(connection)
                .provide<EtableringEgenVirksomhetRepository>()
                .hentHvisEksisterer(behandlingId)
                ?.vurderinger
                .orEmpty()
        }

    private fun hentVurderingIder(behandlingId: BehandlingId): List<Long> =
        dataSource.transaction { connection ->
            connection.queryList(
                "SELECT id FROM etablering_egen_virksomhet_vurdering WHERE vurdert_i_behandling = ? ORDER BY id"
            ) {
                setParams { setLong(1, behandlingId.toLong()) }
                setRowMapper { row -> row.getLong("id") }
            }
        }
}
