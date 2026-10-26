package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetService
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.VirksomhetEtableringGyldig
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBistandRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryEtableringEgenVirksomRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomRepository
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.bistandsbehov.Bistandsvurdering
import no.nav.aap.bistandsbehov.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringFase
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.brukteDager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.tilGammeltFormat
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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

    @Test
    fun `full migrering konverterer aktive og historiske grunnlag og kan kjøres på nytt`() {
        TestDataSource().use { isolertDataSource ->
            val behandlingId = isolertDataSource.transaction { connection ->
                val behandlingId = finnEllerOpprettBehandling(connection, sak(connection)).id
                val repository = postgresRepositoryRegistry.provider(connection)
                    .provide<EtableringEgenVirksomhetRepository>()
                val gammel = vurdering(
                    behandlingId,
                    fom = 1 januar 2025,
                    utviklingsPerioder = listOf(Periode(6 januar 2025, 10 januar 2025)),
                    oppstartsPerioder = listOf(Periode(13 januar 2025, 17 januar 2025)),
                )
                repository.lagre(behandlingId, listOf(gammel))
                repository.lagre(behandlingId, listOf(gammel.copy(begrunnelse = "Ny vurdering")))
                behandlingId
            }
            val backfill = BackfillEtableringVurderinger(isolertDataSource, dryRun = false)
            val resultat = isolertDataSource.transaction { backfill.migrer(it) }

            assertThat(resultat).usingRecursiveComparison().isEqualTo(
                BackfillEtableringVurderinger.Resultat(
                    antallBehandlinger = 1,
                    antallVurderingerSomSplittes = 2,
                    antallNyeVurderinger = 4,
                )
            )
            val iderEtter = isolertDataSource.transaction { connection ->
                val grunnlag = backfill.hentAlleGrunnlag(connection, behandlingId)
                assertThat(grunnlag).hasSize(2)
                grunnlag.flatMap { grunnlagId ->
                    val vurderinger = backfill.vurderinger(connection, grunnlagId)
                    assertThat(vurderinger).hasSize(2)
                    assertThat(vurderinger.map { it.second }.brukteDager()).isEqualTo(5 to 5)
                    assertThat(vurderinger.map { it.second }).allSatisfy {
                        assertThat(it.fase).isNotNull()
                        assertThat(it.utviklingsPerioder).isEmpty()
                        assertThat(it.oppstartsPerioder).isEmpty()
                    }
                    vurderinger.map { it.first }
                }
            }
            val andreKjøring = isolertDataSource.transaction { backfill.migrer(it) }
            assertThat(andreKjøring).usingRecursiveComparison()
                .isEqualTo(BackfillEtableringVurderinger.Resultat(antallBehandlinger = 1))
            isolertDataSource.transaction { connection ->
                val ider = backfill.hentAlleGrunnlag(connection, behandlingId)
                    .flatMap { backfill.vurderinger(connection, it) }.map { it.first }
                assertThat(ider).containsExactlyInAnyOrderElementsOf(iderEtter)
            }
        }
    }

    @Test
    fun `splitter hver periode i egen vurdering med fase`() {
        //                       |des      |jan      |feb      |mar
        // FØR
        // fom                   F
        // utvikling 1                       UUUU
        // utvikling 2                           UUUU
        // oppstart                                   OOOO

        // ETTER
        // => utvikling 1                    UUUU
        // => utvikling 2                        UUUU
        // => oppstart                                OOOO
        // F=fom, U=utvikling, O=oppstart
        val utvikling1 = Periode(6 januar 2025, 15 januar 2025)
        val utvikling2 = Periode(20 januar 2025, 29 januar 2025)
        val oppstart = Periode(3 februar 2025, 12 februar 2025)
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 1 desember 2024,
            utviklingsPerioder = listOf(utvikling1, utvikling2),
            oppstartsPerioder = listOf(oppstart),
            virksomhetErNy = true,
        )
        lagre(behandlingId, legacy)

        migrer(behandlingId)

        assertThat(hentVurderinger(behandlingId)).containsExactlyInAnyOrder(
            legacy.somUtvikling(utvikling1),
            legacy.somUtvikling(utvikling2),
            legacy.somOppstart(oppstart),
        )
        assertValideringUendret(listOf(legacy), hentVurderinger(behandlingId))
    }

    @Test
    fun `dry run endrer ikke databasen og rapporterer hva som ville blitt migrert`() {
        val resultatFør = dataSource.transaction { connection ->
            BackfillEtableringVurderinger(dataSource, dryRun = true).migrer(connection)
        }
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 1 desember 2024,
            utviklingsPerioder = listOf(
                Periode(6 januar 2025, 15 januar 2025),
                Periode(20 januar 2025, 29 januar 2025)
            ),
            oppstartsPerioder = listOf(Periode(3 februar 2025, 12 februar 2025)),
        )
        lagre(behandlingId, legacy)
        val vurderingerFør = hentVurderinger(behandlingId)
        val iderFør = hentVurderingIder(behandlingId)

        val resultat = dataSource.transaction { connection ->
            BackfillEtableringVurderinger(dataSource, dryRun = true).migrer(connection)
        }

        assertThat(hentVurderinger(behandlingId)).isEqualTo(vurderingerFør)
        assertThat(hentVurderingIder(behandlingId)).isEqualTo(iderFør)
        assertThat(resultat.antallVurderingerSomSplittes).isEqualTo(resultatFør.antallVurderingerSomSplittes + 1)
        assertThat(resultat.antallNyeVurderinger).isEqualTo(resultatFør.antallNyeVurderinger + 3)
        assertThat(resultat.antallMedAvvikIVarighet).isEqualTo(resultatFør.antallMedAvvikIVarighet)
    }

    @Test
    fun `avvik i lagret varighet avbryter og ruller tilbake migreringen`() {
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 1 januar 2025,
            utviklingsPerioder = listOf(Periode(6 januar 2025, 15 januar 2025)),
        )
        lagre(behandlingId, legacy)
        val iderFør = hentVurderingIder(behandlingId)

        assertThatThrownBy {
            dataSource.transaction { connection ->
                connection.execute(
                    """
                    CREATE FUNCTION endre_migrert_fase() RETURNS trigger AS ${'$'}${'$'}
                    BEGIN
                        NEW.fase := 'OPPSTART';
                        RETURN NEW;
                    END;
                    ${'$'}${'$'} LANGUAGE plpgsql
                    """.trimIndent()
                )
                connection.execute(
                    """
                    CREATE TRIGGER endre_migrert_fase
                    BEFORE INSERT ON etablering_egen_virksomhet_vurdering
                    FOR EACH ROW EXECUTE FUNCTION endre_migrert_fase()
                    """.trimIndent()
                )

                BackfillEtableringVurderinger(dataSource, dryRun = false).migrerBehandling(connection, behandlingId)
                connection.execute("DROP TRIGGER endre_migrert_fase ON etablering_egen_virksomhet_vurdering")
                connection.execute("DROP FUNCTION endre_migrert_fase()")
            }
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Varighet")
            .hasMessageContaining(behandlingId.id.toString())

        assertThat(hentVurderinger(behandlingId)).containsExactly(legacy)
        assertThat(hentVurderingIder(behandlingId)).isEqualTo(iderFør)
    }

    @Test
    fun `endret valideringsresultat ruller tilbake selv om varigheten er uendret`() {
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 7 januar 2025,
            utviklingsPerioder = listOf(Periode(6 januar 2025, 15 januar 2025)),
            virksomhetErNy = true,
        )
        lagre(behandlingId, legacy)
        val iderFør = hentVurderingIder(behandlingId)
        dataSource.transaction { connection ->
            val provider = postgresRepositoryRegistry.provider(connection)
            provider.provide<SykdomRepository>().lagre(
                behandlingId, listOf(
                    Sykdomsvurdering(
                        begrunnelse = "...",
                        vurderingenGjelderFra = 6 januar 2025,
                        vurderingenGjelderTil = null,
                        harSkadeSykdomEllerLyte = true,
                        harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = false,
                        yrkesskadeBegrunnelse = null,
                        diagnose = null,
                        vurdertIBehandling = behandlingId,
                        opprettet = Instant.now(),
                        vurdertAv = Bruker("saks"),
                    )
                )
            )
            provider.provide<BistandRepository>().lagre(
                behandlingId, listOf(
                    Bistandsvurdering(
                        begrunnelse = "...",
                        erBehovForAktivBehandling = true,
                        erBehovForArbeidsrettetTiltak = true,
                        erBehovForAnnenOppfølging = false,
                        overgangBegrunnelse = null,
                        skalVurdereAapIOvergangTilArbeid = false,
                        vurdertAv = Bruker("saks"),
                        fom = 6 januar 2025,
                        tom = null,
                        opprettet = Instant.now(),
                        vurdertIBehandling = behandlingId,
                    )
                )
            )
            assertThat(EtableringEgenVirksomhetService(provider).erVurderingerGyldig(behandlingId, listOf(legacy)))
                .isEqualTo(VirksomhetEtableringGyldig)
        }
        assertThat(varighet(migrert(legacy))).isEqualTo(varighet(listOf(legacy)))

        assertThatThrownBy { migrer(behandlingId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Valideringsresultat")
            .hasMessageContaining(behandlingId.id.toString())

        assertThat(hentVurderinger(behandlingId)).containsExactly(legacy)
        assertThat(hentVurderingIder(behandlingId)).isEqualTo(iderFør)
    }

    @Test
    fun `analyse teller bare vurderinger som splittes og beholder varighet for uendrede vurderinger`() {
        val behandlingId = opprettBehandling()
        val alleredeMigrert = vurdering(
            behandlingId,
            fom = 8 januar 2025,
            tom = 17 januar 2025,
            fase = EtableringFase.UTVIKLING,
        )
        val legacy = vurdering(
            behandlingId,
            fom = 3 februar 2025,
            oppstartsPerioder = listOf(Periode(10 februar 2025, 19 februar 2025)),
        )
        lagre(behandlingId, alleredeMigrert, legacy)

        val resultat = dataSource.transaction { connection ->
            BackfillEtableringVurderinger(dataSource, dryRun = true).analyserBehandling(connection, behandlingId)
        }

        assertThat(resultat).usingRecursiveComparison().isEqualTo(
            BackfillEtableringVurderinger.Resultat(
                antallBehandlinger = 1,
                antallVurderingerSomSplittes = 1,
                antallNyeVurderinger = 1,
                antallMedAvvikIVarighet = 0,
            )
        )
    }

    @Test
    fun `migrering er idempotent`() {
        //                       |des      |jan      |feb      |mar
        // FØR
        // fom                             F
        // utvikling 1                     UUUU
        // utvikling 2                         UUUUU
        // oppstart                                        OOOO

        // ETTER
        // => utvikling 1                  UUUU
        // => utvikling 2                      UUUUU
        // => oppstart                                     OOOO
        // F=fom, U=utvikling, O=oppstart
        val utvikling1 = Periode(2 januar 2025, 9 januar 2025)
        val utvikling2 = Periode(13 januar 2025, 24 januar 2025)
        val oppstart = Periode(17 februar 2025, 28 februar 2025)
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 2 januar 2025,
            utviklingsPerioder = listOf(utvikling1, utvikling2),
            oppstartsPerioder = listOf(oppstart),
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
        //                       |des      |jan      |feb      |mar
        // FØR
        // fom                                  F
        // utvikling 1                       UUUUU
        // utvikling 2                              UUUU

        // ETTER
        // => utvikling 1                    UUUUU
        // => utvikling 2                           UUUU
        // F=fom, U=utvikling, O=oppstart
        val utvikling1 = Periode(6 januar 2025, 20 januar 2025)
        val utvikling2 = Periode(27 januar 2025, 5 februar 2025)
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 15 januar 2025,
            utviklingsPerioder = listOf(utvikling1, utvikling2),
        )
        lagre(behandlingId, legacy)

        migrer(behandlingId)

        assertThat(hentVurderinger(behandlingId)).containsExactlyInAnyOrder(
            legacy.somUtvikling(utvikling1),
            legacy.somUtvikling(utvikling2),
        )
        assertValideringUendret(listOf(legacy), hentVurderinger(behandlingId))
    }

    @Test
    fun `bruker periodens fom selv om vurderingens fom er før andre periode`() {
        //                       |des      |jan      |feb      |mar
        // FØR
        // fom                                F
        // utvikling 1                        UUUU
        // utvikling 2                             UUUU
        // oppstart                                     OOOO

        // ETTER
        // => utvikling 1                     UUUU
        // => utvikling 2                          UUUU
        // => oppstart                                  OOOO
        // F=fom, U=utvikling, O=oppstart
        val utvikling1 = Periode(10 januar 2025, 19 januar 2025)
        val utvikling2 = Periode(24 januar 2025, 2 februar 2025)
        val oppstart = Periode(10 februar 2025, 19 februar 2025)
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 10 januar 2025,
            utviklingsPerioder = listOf(utvikling1, utvikling2),
            oppstartsPerioder = listOf(oppstart),
        )
        lagre(behandlingId, legacy)

        migrer(behandlingId)

        assertThat(hentVurderinger(behandlingId)).containsExactlyInAnyOrder(
            legacy.somUtvikling(utvikling1),
            legacy.somUtvikling(utvikling2),
            legacy.somOppstart(oppstart),
        )
        assertValideringUendret(listOf(legacy), hentVurderinger(behandlingId))
    }

    @Test
    fun `bruker periodens fom og tom selv om vurderingen har tom som ikke samsvarer med periodene`() {
        //                       |des      |jan      |feb      |mar
        // FØR
        // fom-tom               FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
        // oppstart                                      OOOO

        // ETTER
        // => oppstart                                   OOOO
        // F=fom, U=utvikling, O=oppstart
        val oppstart = Periode(12 februar 2025, 21 februar 2025)
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 1 desember 2024,
            tom = 31 mars 2025,
            oppstartsPerioder = listOf(oppstart),
        )
        lagre(behandlingId, legacy)

        migrer(behandlingId)

        assertThat(hentVurderinger(behandlingId)).containsExactly(legacy.somOppstart(oppstart))
        assertValideringUendret(listOf(legacy), hentVurderinger(behandlingId))
    }

    @Test
    fun `lar vurdering uten perioder være uendret`() {
        //                       |des      |jan      |feb      |mar
        // FØR
        // fom                                   F

        // ETTER
        // => uendret                            F
        // F=fom, U=utvikling, O=oppstart
        val behandlingId = opprettBehandling()
        val utenPerioder = vurdering(behandlingId, fom = 20 januar 2025)
        lagre(behandlingId, utenPerioder)
        val idererFør = hentVurderingIder(behandlingId)

        migrer(behandlingId)

        assertThat(hentVurderinger(behandlingId)).containsExactly(utenPerioder)
        assertThat(hentVurderingIder(behandlingId)).isEqualTo(idererFør)
    }

    @Test
    fun `rører ikke vurderinger som allerede har fase`() {
        //                       |des      |jan      |feb      |mar
        // FØR
        // migrert utvikling                 UUUU
        // legacy fom                                 F
        // legacy oppstart                              OOOO
        // ETTER
        // => oppstart                                  OOOO
        // F=fom, U=utvikling, O=oppstart
        val migrertUtvikling = Periode(8 januar 2025, 17 januar 2025)
        val legacyOppstart = Periode(10 februar 2025, 19 februar 2025)
        val behandlingId = opprettBehandling()
        val alleredeMigrert = vurdering(
            behandlingId,
            fom = migrertUtvikling.fom,
            tom = migrertUtvikling.tom,
            fase = EtableringFase.UTVIKLING,
        )
        val legacy = vurdering(
            behandlingId,
            fom = 3 februar 2025,
            oppstartsPerioder = listOf(legacyOppstart),
            virksomhetNavn = "Legacy",
        )
        lagre(behandlingId, alleredeMigrert, legacy)
        val idTilAlleredeMigrert = hentVurderingIder(behandlingId).first()

        migrer(behandlingId)

        assertThat(hentVurderinger(behandlingId)).containsExactlyInAnyOrder(
            alleredeMigrert,
            legacy.somOppstart(legacyOppstart),
        )
        assertThat(hentVurderingIder(behandlingId)).contains(idTilAlleredeMigrert)
    }

    @Test
    fun `total varighet er uendret for en behandling`() {
        //                       |des      |jan      |feb      |mar
        // FØR
        // fom                             F
        // utvikling 1                       UUUU
        // utvikling 2                           UUUUU
        // oppstart                                   OOOOO
        // ETTER
        // => utvikling 1                    UUUU
        // => utvikling 2                        UUUUU
        // => oppstart                                OOOOO
        // F=fom, U=utvikling, O=oppstart
        val behandlingId = opprettBehandling()
        val legacy = vurdering(
            behandlingId,
            fom = 1 januar 2025,
            utviklingsPerioder = listOf(
                Periode(6 januar 2025, 17 januar 2025), Periode(20 januar 2025, 31 januar 2025)
            ),
            oppstartsPerioder = listOf(Periode(3 februar 2025, 14 februar 2025)),
        )

        assertThat(varighet(migrert(legacy))).isEqualTo(varighet(listOf(legacy)))
        assertValideringUendret(listOf(legacy), migrert(legacy))
    }

    @Test
    fun `senere vurdering overstyrer utviklingsperioder som starter etter dens fom`() {
        //                       |des      |jan      |feb      |mar
        // FØR
        // eldre fom                       F
        // eldre utvikling 1                 UUUU
        // eldre utvikling 2                                   UUUU
        // nyere fom                                 F

        // ETTER
        // => utvikling 1                    UUUU
        // => utvikling 2                                      UUUU (overstyres av nyere)
        // => nyere (uten perioder)                    F
        // F=fom, U=utvikling, O=oppstart
        val førsteBehandling = opprettBehandling()
        val andreBehandling = opprettBehandling()
        val eldre = vurdering(
            førsteBehandling,
            fom = 1 januar 2025,
            utviklingsPerioder = listOf(Periode(6 januar 2025, 15 januar 2025), Periode(3 mars 2025, 12 mars 2025)),
        )
        val nyere = vurdering(andreBehandling, fom = 1 februar 2025, opprettet = Instant.parse("2025-02-01T00:00:00Z"))

        assertThat(varighet(migrert(eldre) + nyere)).isEqualTo(8 to 0)
        assertValideringUendret(listOf(eldre, nyere), migrert(eldre) + nyere)
    }

    @Test
    fun `total varighet og evaluering er uendret for liste med både oppfylte og ikke-oppfylte vurderinger`() {
        //                       |des      |jan      |feb      |mar
        // FØR
        // oppfylt fom                     F
        // oppfylt utvikling 1               UUUU
        // oppfylt utvikling 2                   UUUUU
        // ikke oppfylt fom                           F
        // ikke oppfylt oppstart                        OOOO
        // uten perioder fom                                   F
        // ETTER
        // => oppfylt utv. 1                 UUUU
        // => oppfylt utv. 2                     UUUUU
        // => ikke oppfylt oppstart                       OOOO
        // => uten perioder                                    F
        // F=fom, U=utvikling, O=oppstart
        val behandlingId = opprettBehandling()
        val oppfylt = vurdering(
            behandlingId,
            fom = 2 januar 2025,
            utviklingsPerioder = listOf(
                Periode(6 januar 2025, 17 januar 2025), Periode(20 januar 2025, 31 januar 2025)
            ),
            virksomhetErNy = true,
        )
        val ikkeOppfylt = vurdering(
            behandlingId,
            fom = 3 februar 2025,
            oppstartsPerioder = listOf(Periode(10 februar 2025, 19 februar 2025)),
            virksomhetErNy = false,
        )
        val utenPerioder = vurdering(behandlingId, fom = 3 mars 2025, virksomhetErNy = false)
        val før = listOf(oppfylt, ikkeOppfylt, utenPerioder)
        val etter = før.flatMap { migrert(it) }

        assertThat(evalueringer(før)).containsExactly(true, false, false)
        assertThat(varighet(etter)).isEqualTo(varighet(før))
        assertThat(etter.map { it.evaluerVirksomhetVurdering() }).containsExactlyInAnyOrder(true, true, false, false)
        assertValideringUendret(før, etter)
    }

    @Test
    fun `reproduser prod 1`() {
        //                         26.06      03.08 04.08       03.11 04.11       25.12 26.12       25.03
        // FØR
        // nyere vurdering        UUUUUUUUUUUUU OOOOOOOOOOOOOOOOOOOO
        // eldre vurdering        xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
        //
        // ETTER MIGRERING
        // nyere vurdering        UUUUUUUUUUUUU OOOOOOOOOOOOOOOOOOOO
        // eldre vurdering                                                  UUUUUUUUUUUUU OOOOOOOOOOOOO
        //
        // Den eldre vurderingen er overstyrt fra 26.06, men utvikling og oppstart gjenoppstår etter 03.11.
        // U=utvikling, O=oppstart, x=overstyrt
        val behandlingId = opprettBehandling()
        val behandlingId2 = opprettBehandling()
        val vurderinger = listOf(
            vurdering(
                behandlingId = behandlingId,
                fom = 26 juni 2026,
                tom = null,
                utviklingsPerioder = listOf(Periode(26 juni 2026, 25 desember 2026)),
                oppstartsPerioder = listOf(Periode(26 desember 2026, 25 mars 2027)),
                fase = null,
                virksomhetNavn = "....",
                opprettet = (26 juni 2026).atStartOfDay().toInstant(ZoneOffset.UTC),
                virksomhetErNy = true
            ),
            vurdering(
                behandlingId = behandlingId2,
                fom = 26 juni 2026,
                tom = null,
                utviklingsPerioder = listOf(Periode(26 juni 2026, 3 august 2026)),
                oppstartsPerioder = listOf(Periode(4 august 2026, 3 november 2026)),
                fase = null,
                virksomhetNavn = "...",
                opprettet = (14 oktober 2026).atStartOfDay().toInstant(ZoneOffset.UTC),
                virksomhetErNy = true
            )
        )

        lagre(behandlingId2, *vurderinger.toTypedArray())
        migrer(behandlingId2)
        val etter = hentVurderinger(behandlingId2)

        assertThat(varighet(etter)).isEqualTo(varighet(vurderinger))

    }


    @Test
    fun `reproduser prod 2`() {
        //                         26.06      03.08 04.08                       25.12 26.12       25.03
        // FØR
        // nyere vurdering        UUUUUUUUUUUUU
        // eldre vurdering        xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
        //
        // ETTER MIGRERING
        // nyere vurdering        UUUUUUUUUUUUU
        // eldre vurdering                      UUUUUUUUUUUUUUUUUUUUUUUUUUUUU OOOOOOOOOOOOO
        //
        // Den nyere vurderingen har ingen oppstart, men migreringen lar den eldre vurderingen gjenoppstå.
        // U=utvikling, O=oppstart, x=overstyrt
        val behandlingId = opprettBehandling()
        val behandlingId2 = opprettBehandling()
        val vurderinger = listOf(
            vurdering(
                behandlingId = behandlingId,
                fom = 26 juni 2026,
                tom = null,
                utviklingsPerioder = listOf(Periode(26 juni 2026, 25 desember 2026)),
                oppstartsPerioder = listOf(Periode(26 desember 2026, 25 mars 2027)),
                fase = null,
                virksomhetNavn = "....",
                opprettet = (26 juni 2026).atStartOfDay().toInstant(ZoneOffset.UTC),
                virksomhetErNy = true
            ),
            vurdering(
                behandlingId = behandlingId2,
                fom = 26 juni 2026,
                tom = null,
                utviklingsPerioder = listOf(Periode(26 juni 2026, 3 august 2026)),
                oppstartsPerioder = listOf(),
                fase = null,
                virksomhetNavn = "...",
                opprettet = (28 august 2026).atStartOfDay().toInstant(ZoneOffset.UTC),
                virksomhetErNy = true
            )
        )

        lagre(behandlingId2, *vurderinger.toTypedArray())
        migrer(behandlingId2)
        val etter = hentVurderinger(behandlingId2)

        assertThat(varighet(etter)).isEqualTo(varighet(vurderinger))

    }

    private val service = EtableringEgenVirksomhetService(
        InMemoryEtableringEgenVirksomRepository,
        InMemoryBehandlingRepository,
        InMemoryBistandRepository,
        InMemorySykdomRepository
    )

    private fun assertValideringUendret(
        før: List<EtableringEgenVirksomhetVurdering>,
        etter: List<EtableringEgenVirksomhetVurdering>,
    ) {
        val (_, behandling) = opprettInMemorySakOgBehandling(1 desember 2024)
        val oppfyltFra = 30 november 2024
        InMemorySykdomRepository.lagre(
            behandling.id, listOf(
                Sykdomsvurdering(
                    begrunnelse = "...",
                    vurderingenGjelderFra = oppfyltFra,
                    vurderingenGjelderTil = null,
                    harSkadeSykdomEllerLyte = true,
                    harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = false,
                    yrkesskadeBegrunnelse = null,
                    diagnose = null,
                    vurdertIBehandling = behandling.id,
                    opprettet = Instant.now(),
                    vurdertAv = Bruker("saks")
                )
            )
        )
        InMemoryBistandRepository.lagre(
            behandling.id, listOf(
                Bistandsvurdering(
                    begrunnelse = "...",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = true,
                    erBehovForAnnenOppfølging = false,
                    overgangBegrunnelse = null,
                    skalVurdereAapIOvergangTilArbeid = false,
                    vurdertAv = Bruker("saks"),
                    fom = oppfyltFra,
                    tom = null,
                    opprettet = Instant.now(),
                    vurdertIBehandling = behandling.id
                )
            )
        )

        val gyldigFør = service.erVurderingerGyldig(behandling.id, før)
        val gyldigEtter = service.erVurderingerGyldig(behandling.id, etter)

        assertThat(gyldigFør).isEqualTo(VirksomhetEtableringGyldig)
        assertThat(gyldigEtter).isEqualTo(gyldigFør)
    }

    private fun evalueringer(vurderinger: List<EtableringEgenVirksomhetVurdering>) =
        vurderinger.map { it.evaluerVirksomhetVurdering() }

    private fun migrert(vurdering: EtableringEgenVirksomhetVurdering) =
        BackfillEtableringVurderinger(dataSource, dryRun = false).oppdaterVurdering(vurdering)
            .ifEmpty { listOf(vurdering) }

    private fun varighet(vurderinger: List<EtableringEgenVirksomhetVurdering>): Pair<Int, Int> =
        vurderinger
            .map { if (it.fase != null) it.tilGammeltFormat() else it }
            .brukteDager()

    private fun EtableringEgenVirksomhetVurdering.somUtvikling(periode: Periode) = copy(
        utviklingsPerioder = emptyList(),
        oppstartsPerioder = emptyList(),
        fase = EtableringFase.UTVIKLING,
        fom = periode.fom,
        tom = periode.tom,
    )

    private fun EtableringEgenVirksomhetVurdering.somOppstart(periode: Periode) = copy(
        utviklingsPerioder = emptyList(),
        oppstartsPerioder = emptyList(),
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
            postgresRepositoryRegistry.provider(connection).provide<EtableringEgenVirksomhetRepository>()
                .lagre(behandlingId, vurderinger.toList())
        }
    }

    private fun migrer(behandlingId: BehandlingId) {
        val backfill = BackfillEtableringVurderinger(dataSource, dryRun = false)
        dataSource.transaction { connection ->
            backfill.migrerBehandling(connection, behandlingId)
        }
    }

    private fun hentVurderinger(behandlingId: BehandlingId): List<EtableringEgenVirksomhetVurdering> =
        dataSource.transaction { connection ->
            postgresRepositoryRegistry.provider(connection).provide<EtableringEgenVirksomhetRepository>()
                .hentHvisEksisterer(behandlingId)?.vurderinger.orEmpty()
        }

    private fun hentVurderingIder(behandlingId: BehandlingId): List<Long> = dataSource.transaction { connection ->
        connection.queryList(
            "SELECT id FROM etablering_egen_virksomhet_vurdering WHERE vurdert_i_behandling = ? ORDER BY id"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row -> row.getLong("id") }
        }
    }
}
