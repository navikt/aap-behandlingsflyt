package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingVurdering
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingÅrsak
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.avbrytrevurdering.AvbrytRevurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate


internal class SamordningVurderingRepositoryImplTest {
    companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `lagre og hente ut igjen`() {
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak(it)) }

        // Lagre vurdering
        val vurdering = SamordningVurdering(
            ytelseType = Ytelse.SYKEPENGER,
            vurderingPerioder = setOf(
                SamordningVurderingPeriode(
                    periode = Periode(LocalDate.now().minusYears(3), LocalDate.now().minusDays(1)),
                    gradering = Prosent(40),
                    kronesum = null,
                    manuell = false,
                ),
                SamordningVurderingPeriode(
                    periode = Periode(LocalDate.now().minusYears(6), LocalDate.now().minusYears(5)),
                    gradering = Prosent(40),
                    kronesum = null,
                    manuell = false,
                )
            )
        )

        // Lagre vurdering
        val vurdering2 = SamordningVurdering(
            ytelseType = Ytelse.OPPLÆRINGSPENGER,
            vurderingPerioder = setOf(
                SamordningVurderingPeriode(
                    periode = Periode(LocalDate.now().minusYears(3), LocalDate.now().minusDays(1)),
                    gradering = Prosent(40),
                    kronesum = null,
                    manuell = false,
                ),
                SamordningVurderingPeriode(
                    periode = Periode(LocalDate.now().minusYears(6), LocalDate.now().minusYears(5)),
                    gradering = Prosent(40),
                    kronesum = null,
                    manuell = false,
                )
            )
        )
        dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).lagreVurderinger(
                behandlingId = behandling.id,
                samordningVurderinger = SamordningVurderingGrunnlag(
                    begrunnelse = "En god begrunnelse",
                    maksDatoEndelig = false,
                    fristNyRevurdering = LocalDate.now().plusYears(1),
                    vurderinger = setOf(vurdering, vurdering2),
                    vurdertAv = "ident"
                )
            )
        }

        val uthentet = dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet?.vurderinger).containsExactlyInAnyOrder(vurdering, vurdering2)
    }

    @Test
    fun `lagre en vurdering uten perioder`() {
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak(it)) }

        dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).lagreVurderinger(
                behandling.id, SamordningVurderingGrunnlag(
                    begrunnelse = "xxxx",
                    maksDatoEndelig = true,
                    fristNyRevurdering = LocalDate.now().plusYears(1),
                    vurderinger = emptySet(),
                    vurdertAv = "ident"
                )
            )
        }

        val uthentet = dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet?.begrunnelse).isEqualTo("xxxx")
        assertThat(uthentet?.maksDatoEndelig).isTrue()
        assertThat(uthentet?.fristNyRevurdering).isEqualTo(LocalDate.now().plusYears(1))
    }

    @Test
    fun `å lagre en vurdering før ytelse eksisterer gir ikke feil`() {
        val behandling = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak(it))
        }

        // Lagre vurdering
        val vurdering = SamordningVurdering(
            ytelseType = Ytelse.SYKEPENGER,

            vurderingPerioder = setOf(
                SamordningVurderingPeriode(
                    periode = Periode(LocalDate.now().minusYears(3), LocalDate.now().minusDays(1)),
                    gradering = Prosent(40),
                    kronesum = null,
                    manuell = false,
                )
            )
        )
        assertDoesNotThrow {
            dataSource.transaction {
                SamordningVurderingRepositoryImpl(it).lagreVurderinger(
                    behandlingId = behandling.id,
                    samordningVurderinger = SamordningVurderingGrunnlag(
                        begrunnelse = "En god begrunnelse",
                        maksDatoEndelig = false,
                        fristNyRevurdering = LocalDate.now().plusYears(1),
                        vurderinger = setOf(vurdering),
                        vurdertAv = "ident"
                    )
                )
            }
        }
    }

    @Test
    fun `lagre flere vurderinger og verifiser at nyeste hentes ut`() {
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak(it)) }

        // Create fixed dates for the periods to avoid test flakiness
        val førstePeriodeStart = LocalDate.of(2022, 1, 1)
        val førstePeriodeEnd = LocalDate.of(2023, 1, 1)
        val andrePeriodeStart = LocalDate.of(2024, 1, 1)
        val andrePeriodeEnd = LocalDate.of(2025, 1, 1)

        // Create the first vurdering
        val førsteVurdering = SamordningVurdering(
            ytelseType = Ytelse.SYKEPENGER,
            vurderingPerioder = setOf(
                SamordningVurderingPeriode(
                    periode = Periode(førstePeriodeStart, førstePeriodeEnd),
                    gradering = Prosent(40),
                    kronesum = null,
                    manuell = false,
                )
            )
        )

        // Save the first vurdering
        dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).lagreVurderinger(
                behandlingId = behandling.id,
                samordningVurderinger = SamordningVurderingGrunnlag(
                    begrunnelse = "Første begrunnelse",
                    maksDatoEndelig = false,
                    fristNyRevurdering = LocalDate.of(2025, 1, 1),
                    vurderinger = setOf(førsteVurdering),
                    vurdertAv = "ident"
                )
            )
        }

        // Create the second vurdering
        val andreVurdering1 = SamordningVurdering(
            ytelseType = Ytelse.FORELDREPENGER,
            vurderingPerioder = setOf(
                SamordningVurderingPeriode(
                    periode = Periode(andrePeriodeStart, andrePeriodeEnd),
                    gradering = Prosent(50),
                    manuell = false,
                )
            )
        )
        val andreVurdering2 = SamordningVurdering(
            ytelseType = Ytelse.OPPLÆRINGSPENGER,
            vurderingPerioder = setOf(
                SamordningVurderingPeriode(
                    periode = Periode(andrePeriodeStart.plusDays(1), andrePeriodeStart.plusMonths(6)),
                    gradering = Prosent(30),
                    manuell = false,
                ),
                SamordningVurderingPeriode(
                    periode = Periode(andrePeriodeStart.plusMonths(7), andrePeriodeEnd.plusDays(6)),
                    gradering = Prosent(33),
                    manuell = false,
                )
            )
        )

        // Save the second vurdering
        val andreBegrunnelse = "Andre begrunnelse"
        val andreMaksDato = LocalDate.of(2026, 1, 1)

        dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).lagreVurderinger(
                behandlingId = behandling.id,
                samordningVurderinger = SamordningVurderingGrunnlag(
                    begrunnelse = andreBegrunnelse,
                    maksDatoEndelig = true,
                    fristNyRevurdering = andreMaksDato,
                    vurderinger = setOf(andreVurdering1, andreVurdering2),
                    vurdertAv = "ident"
                )
            )
        }

        // Retrieve the vurdering
        val uthentet = dataSource.transaction {
            SamordningVurderingRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }!!

        // Verify that the retrieved vurdering has the expected properties
        assertThat(uthentet.begrunnelse).isEqualTo(andreBegrunnelse)
        // Verify that the retrieved vurdering has the expected number of vurderinger
        assertThat(uthentet.vurderinger).hasSize(2)

        // Verify that the retrieved vurderinger have the expected ytelse types
        val actualVurderinger = uthentet.vurderinger

        // Find the vurderinger by their ytelse types
        val foreldrepengerVurdering = actualVurderinger.find { it.ytelseType == Ytelse.FORELDREPENGER }
        val opplæringspengerVurdering = actualVurderinger.find { it.ytelseType == Ytelse.OPPLÆRINGSPENGER }

        // Verify that both vurderinger were found
        assertThat(foreldrepengerVurdering).isNotNull()
        assertThat(opplæringspengerVurdering).isNotNull()

        // Verify the first vurdering (FORELDREPENGER)
        assertThat(foreldrepengerVurdering?.ytelseType).isEqualTo(Ytelse.FORELDREPENGER)
        assertThat(foreldrepengerVurdering?.vurderingPerioder).hasSize(1)

        // Verify the properties of the first vurdering's periode
        val foreldrepengerPeriode = foreldrepengerVurdering?.vurderingPerioder?.firstOrNull()
        assertThat(foreldrepengerPeriode?.periode?.fom).isEqualTo(andrePeriodeStart)
        assertThat(foreldrepengerPeriode?.periode?.tom).isEqualTo(andrePeriodeEnd)
        assertThat(foreldrepengerPeriode?.gradering?.prosentverdi()).isEqualTo(50)
        assertThat(foreldrepengerPeriode?.kronesum).isNull()
        assertThat(foreldrepengerPeriode?.manuell).isFalse()

        // Verify the second vurdering (OPPLÆRINGSPENGER)
        assertThat(opplæringspengerVurdering?.ytelseType).isEqualTo(Ytelse.OPPLÆRINGSPENGER)
        assertThat(opplæringspengerVurdering?.vurderingPerioder).hasSize(2)

        assertThat(opplæringspengerVurdering?.vurderingPerioder).anySatisfy {
            assertThat(it.periode.fom).isEqualTo(andrePeriodeStart.plusDays(1))
            assertThat(it.periode.tom).isEqualTo(andrePeriodeStart.plusMonths(6))
            assertThat(it.gradering?.prosentverdi()).isEqualTo(30)
            assertThat(it.kronesum).isNull()
            assertThat(it.manuell).isFalse()
        }

        assertThat(opplæringspengerVurdering?.vurderingPerioder).anySatisfy {
            assertThat(it.periode.fom).isEqualTo(andrePeriodeStart.plusMonths(7))
            assertThat(it.periode.tom).isEqualTo(andrePeriodeEnd.plusDays(6))
            assertThat(it.gradering?.prosentverdi()).isEqualTo(33)
            assertThat(it.kronesum).isNull()
            assertThat(it.manuell).isFalse()
        }
    }

    @Test
    fun `test sletting`() {
        TestDataSource().use { dataSource ->
            dataSource.transaction { connection ->
                val sak = sak(connection)
                val behandling = finnEllerOpprettBehandling(connection, sak)
                val samordningVurderingRepository = SamordningVurderingRepositoryImpl(connection)
                samordningVurderingRepository.lagreVurderinger(
                    behandling.id, SamordningVurderingGrunnlag(
                        begrunnelse = "begrunnelse1",
                        maksDatoEndelig = false,
                        fristNyRevurdering = null,
                        vurdertAv = "ident",
                        vurderinger = setOf(
                            SamordningVurdering(
                                ytelseType = Ytelse.SYKEPENGER,
                                vurderingPerioder = setOf(
                                    SamordningVurderingPeriode(
                                        periode = Periode(5 januar 2024, 10 januar 2024),
                                        gradering = Prosent.`50_PROSENT`,
                                        manuell = false,
                                    )
                                )
                            )
                        )
                    )
                )
                samordningVurderingRepository.lagreVurderinger(
                    behandling.id, SamordningVurderingGrunnlag(
                        begrunnelse = "begrunnelse2",
                        maksDatoEndelig = false,
                        fristNyRevurdering = null,
                        vurdertAv = "ident",
                        vurderinger = setOf(
                            SamordningVurdering(
                                ytelseType = Ytelse.SYKEPENGER,
                                vurderingPerioder = setOf(
                                    SamordningVurderingPeriode(
                                        periode = Periode(11 januar 2024, 15 januar 2024),
                                        gradering = Prosent.`50_PROSENT`,
                                        manuell = false,
                                    )
                                )
                            )
                        )
                    )
                )
                assertDoesNotThrow {
                    samordningVurderingRepository.slett(behandling.id)
                }
            }
        }
    }

    @Test
    fun `historikk viser kun vurderinger fra tidligere behandlinger og ikke inkluderer vurdering fra avbrutt revurdering`() {
        val samordningGrunnlag1 = lagSamordningGrunnlag("B1", "Z00001", Ytelse.SYKEPENGER)
        val samordningGrunnlag2 = lagSamordningGrunnlag("B2", "Z00002", Ytelse.FORELDREPENGER)
        val samordningGrunnlag3 = lagSamordningGrunnlag("B3", "Z00003", Ytelse.OMSORGSPENGER)

        val førstegangsbehandling = dataSource.transaction { connection ->
            val samordningRepo = SamordningVurderingRepositoryImpl(connection)
            val sak = sak(connection)
            val førstegangsbehandling = finnEllerOpprettBehandling(connection, sak)

            samordningRepo.lagreVurderinger(førstegangsbehandling.id, samordningGrunnlag1)
            førstegangsbehandling
        }

        dataSource.transaction { connection ->
            val samordningRepo = SamordningVurderingRepositoryImpl(connection)
            val avbrytRevurderingRepo = AvbrytRevurderingRepositoryImpl(connection)
            val revurderingAvbrutt = revurderingSamordning(connection, førstegangsbehandling)

            // Marker revurderingen som avbrutt
            avbrytRevurderingRepo.lagre(
                revurderingAvbrutt.id, AvbrytRevurderingVurdering(
                    AvbrytRevurderingÅrsak.REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL, "avbryte pga. feil",
                    Bruker("Z00001")
                )
            )
            samordningRepo.lagreVurderinger(revurderingAvbrutt.id, samordningGrunnlag2)
        }

        dataSource.transaction { connection ->
            val samordningRepo = SamordningVurderingRepositoryImpl(connection)
            val revurdering = revurderingSamordning(connection, førstegangsbehandling)

            samordningRepo.lagreVurderinger(revurdering.id, samordningGrunnlag3)

            val historikk = samordningRepo.hentHistoriskeVurderinger(revurdering.sakId, revurdering.id)
            assertThat(historikk)
                .usingRecursiveComparison()
                .ignoringFields("vurderingerId", "vurdertTidspunkt")
                .isEqualTo(listOf(samordningGrunnlag1))
        }
    }

    private fun lagSamordningGrunnlag(
        begrunnelse: String,
        vurdertAv: String,
        ytelse: Ytelse
    ): SamordningVurderingGrunnlag {
        return SamordningVurderingGrunnlag(
            begrunnelse = begrunnelse,
            maksDatoEndelig = false,
            fristNyRevurdering = null,
            vurdertAv = vurdertAv,
            vurderinger = setOf(
                SamordningVurdering(
                    ytelseType = ytelse,
                    vurderingPerioder = setOf(
                        SamordningVurderingPeriode(
                            periode = Periode(11 januar 2024, 15 januar 2024),
                            gradering = Prosent.`50_PROSENT`,
                            manuell = false,
                        )
                    )
                )
            )
        )
    }

    private fun revurderingSamordning(connection: DBConnection, behandling: Behandling): Behandling {
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            behandling.sakId,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = behandling.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.SAMORDNING_OG_AVREGNING)),
                årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE
            )
        )
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(
            ident(),
            periode
        )
    }
}
