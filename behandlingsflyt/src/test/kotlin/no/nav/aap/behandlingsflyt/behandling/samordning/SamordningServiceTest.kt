package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering.SamordningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SamordningServiceTest {
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

    @Test
    fun `gjør vurderinger når all data er tilstede`() {
        val behandlingId = dataSource.transaction { opprettSakdata(it) }
        dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningVurderingRepositoryImpl(connection)
            val samordningYtelseRepository = SamordningYtelseRepositoryImpl(connection)
            opprettYtelseData(samordningYtelseRepository, behandlingId)
            opprettVurderingData(ytelseVurderingRepo, behandlingId)
        }

        dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningVurderingRepositoryImpl(connection)
            val samordningYtelseRepository = SamordningYtelseRepositoryImpl(connection)
            val service = SamordningService(ytelseVurderingRepo, samordningYtelseRepository)

            val hentedeVurderinger = service.hentVurderinger(behandlingId)
            val hentedeYtelser = service.hentYtelser(behandlingId)
            val tidligereVurderinger = service.vurderingTidslinje(hentedeVurderinger)
            assertThat(service.vurder(hentedeYtelser, tidligereVurderinger).segmenter()).isNotEmpty
        }
    }

    @Test
    fun `sammenlign perioder med registerdata, finner ikke-vurderte perioder`() {
        val behandlingId = dataSource.transaction { opprettSakdata(it) }

        // Opprett registerdata med vurdering fra 1 januar til 10 januar
        dataSource.transaction { connection ->
            opprettYtelseData(
                SamordningYtelseRepositoryImpl(connection), behandlingId, ytelser = setOf(
                    SamordningYtelse(
                        ytelseType = Ytelse.SYKEPENGER,
                        ytelsePerioder = setOf(
                            SamordningYtelsePeriode(
                                periode = Periode(1 januar 2024, 10 januar 2024),
                                gradering = Prosent.`70_PROSENT`,
                            )
                        ),
                        kilde = "kilde",
                    )
                )
            )
        }

        // Registrer vurdering fra 5 januar til 10 januar
        dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningVurderingRepositoryImpl(connection)
            opprettVurderingData(
                ytelseVurderingRepo, behandlingId, vurderinger = SamordningVurderingGrunnlag(
                    begrunnelse = "En god begrunnelse",
                    maksDatoEndelig = false,
                    fristNyRevurdering = LocalDate.now().plusYears(1),
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
        }

        val (ytelser, vurderinger) = dataSource.transaction { connection ->
            val service = SamordningService(
                SamordningVurderingRepositoryImpl(connection),
                SamordningYtelseRepositoryImpl(connection)
            )
            Pair(service.hentYtelser(behandlingId), service.hentVurderinger(behandlingId))
        }
        val ikkeVurdertePerioder = dataSource.transaction { connection ->
            val service = SamordningService(
                SamordningVurderingRepositoryImpl(connection),
                SamordningYtelseRepositoryImpl(connection)
            )

            val tidligereVurderinger = service.vurderingTidslinje(vurderinger)
            service.perioderSomIkkeHarBlittVurdert(ytelser, tidligereVurderinger)
        }

        // Forvent at ikke-vurderte perioder er fra 1 jan til 4 jan
        assertThat(ikkeVurdertePerioder.segmenter()).hasSize(1)
        assertThat(ikkeVurdertePerioder.segmenter().first().periode).isEqualTo(
            Periode(1 januar 2024, 4 januar 2024)
        )
    }

    @Test
    fun `gjør vurderinger med overlappende perioder`() {
        val behandlingId = dataSource.transaction { opprettSakdata(it) }
        dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningVurderingRepositoryImpl(connection)
            opprettVurderingData(ytelseVurderingRepo, behandlingId)
        }

        dataSource.transaction { connection ->
            val ytelseVurderingRepo = SamordningVurderingRepositoryImpl(connection)
            val samordningYtelseRepository = SamordningYtelseRepositoryImpl(connection)
            val service = SamordningService(ytelseVurderingRepo, samordningYtelseRepository)

            val grunnlag = SamordningYtelseGrunnlag(
                1L,
                setOf(
                    SamordningYtelse(
                        Ytelse.SYKEPENGER,
                        setOf(
                            SamordningYtelsePeriode(
                                Periode(13 mars 2025, 31 mars 2025),
                                Prosent.`70_PROSENT`,
                                kronesum = null
                            ),
                            SamordningYtelsePeriode(
                                Periode(13 mars 2025, 15 mars 2025),
                                Prosent.`50_PROSENT`,
                                kronesum = null
                            )
                        ),
                        kilde = "kilde"
                    )
                ),
            )

            val vurderinger = SamordningVurderingGrunnlag(
                begrunnelse = "En god begrunnelse",
                maksDatoEndelig = false,
                fristNyRevurdering = LocalDate.now().plusYears(1),
                vurdertAv = "ident",
                vurderinger = setOf(
                    SamordningVurdering(
                        Ytelse.SYKEPENGER,
                        setOf(
                            SamordningVurderingPeriode(
                                Periode(13 mars 2025, 15 mars 2025),
                                Prosent(50),
                                null,
                                true
                            ),
                            SamordningVurderingPeriode(
                                Periode(16 mars 2025, 31 mars 2025),
                                Prosent(70),
                                null,
                                true
                            )
                        )
                    )
                )
            )

            val tidligereVurderinger = service.vurderingTidslinje(vurderinger)
            val samordningGradering = service.vurder(grunnlag, tidligereVurderinger).segmenter().toList()

            assertThat(samordningGradering).hasSize(2)

            with(samordningGradering[0]) {
                assertThat(periode).isEqualTo(Periode(13 mars 2025, 15 mars 2025))
                assertThat(verdi.gradering).isEqualTo(Prosent(50))
            }

            with(samordningGradering[1]) {
                assertThat(periode).isEqualTo(Periode(16 mars 2025, 31 mars 2025))
                assertThat(verdi.gradering).isEqualTo(Prosent(70))
            }
        }
    }

    @Test
    fun `ytelser fra register med overlappende segmenter slås sammen ved sjekk av manglende vurderinger`() {
        dataSource.transaction { connection ->
            val service = SamordningService(
                SamordningVurderingRepositoryImpl(connection),
                SamordningYtelseRepositoryImpl(connection)
            )

            val grunnlag = SamordningYtelseGrunnlag(
                1L,
                setOf(
                    SamordningYtelse(
                        Ytelse.SYKEPENGER,
                        setOf(
                            SamordningYtelsePeriode(
                                Periode(1 januar 2024, 10 januar 2024),
                                Prosent.`70_PROSENT`,
                                kronesum = null
                            ),
                            SamordningYtelsePeriode(
                                Periode(9 januar 2024, 13 januar 2024),
                                Prosent.`50_PROSENT`,
                                kronesum = null
                            )
                        ),
                        kilde = "kilde"
                    )
                ),
            )

            val ikkeVurdertePerioder = service.perioderSomIkkeHarBlittVurdert(grunnlag, Tidslinje.empty())

            assertThat(ikkeVurdertePerioder.segmenter().first().periode).isEqualTo(
                Periode(1 januar 2024, 13 januar 2024)
            )
        }
    }

    private fun opprettVurderingData(
        samordningVurderingRepo: SamordningVurderingRepositoryImpl,
        behandlingId: BehandlingId,
        vurderinger: SamordningVurderingGrunnlag = SamordningVurderingGrunnlag(
            begrunnelse = "En god begrunnelse",
            maksDatoEndelig = false,
            fristNyRevurdering = LocalDate.now().plusYears(1),
            vurdertAv = "ident",
            vurderinger = setOf(
                SamordningVurdering(
                    Ytelse.SYKEPENGER,
                    setOf(
                        SamordningVurderingPeriode(
                            Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                            Prosent(50),
                            0,
                            false
                        )
                    )
                )
            )
        )
    ) {
        samordningVurderingRepo.lagreVurderinger(behandlingId, vurderinger)
    }

    private fun opprettYtelseData(
        samordningYtelseRepo: SamordningYtelseRepositoryImpl,
        behandlingId: BehandlingId,
        ytelser: Set<SamordningYtelse> = setOf(
            SamordningYtelse(
                Ytelse.SYKEPENGER,
                setOf(
                    SamordningYtelsePeriode(
                        Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
                        Prosent(50),
                        0
                    )
                ),
                "kilde",
                "ref"
            )
        )
    ) {
        samordningYtelseRepo.lagre(behandlingId, ytelser)
    }

    private fun opprettSakdata(connection: DBConnection): BehandlingId {
        val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("ident", true)))
        val sakId = SakRepositoryImpl(connection).finnEllerOpprett(
            person,
            Periode(LocalDate.now(), LocalDate.now().plusDays(5))
        ).id
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            sakId,
            TypeBehandling.Førstegangsbehandling,
            null,
            VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        ).id
    }
}