package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingInformasjonskrav.Companion.harEndringerIYtelserIkkeDekketAvManuelleVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingInformasjonskrav.Companion.harEndringerIYtelserIkkeDekketAvEksisterendeGrunnlag
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class SamordningYtelseRepositoryImplTest {
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
    fun `sette inn flere ytelser, skal hente ut nyeste`() {
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak(it)) }

        val samordningYtelser = setOf(
            SamordningYtelse(
                ytelseType = Ytelse.SYKEPENGER,
                ytelsePerioder = setOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
                        gradering = Prosent.`70_PROSENT`,
                    ),
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusYears(7).plusDays(1), LocalDate.now().plusYears(10)),
                        gradering = Prosent.`30_PROSENT`,
                    )
                ),
                kilde = "kilde",
                saksRef = "abc"
            ),
            SamordningYtelse(
                ytelseType = Ytelse.OMSORGSPENGER,
                ytelsePerioder = setOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(6)),
                        gradering = Prosent.`50_PROSENT`,
                    ),
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusYears(7).plusDays(1), LocalDate.now().plusYears(10)),
                        gradering = Prosent.`66_PROSENT`
                    )
                ),
                kilde = "kilde",
                saksRef = "abc"
            )
        )
        dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).lagre(
                behandling.id,
                samordningYtelser = samordningYtelser
            )
        }

        val uthentet = dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet!!.ytelser.size).isEqualTo(2)
        assertThat(samordningYtelser).containsExactlyInAnyOrderElementsOf(uthentet.ytelser)

        // Setter inn på nytt
        val samordningYtelser2 = setOf(
            SamordningYtelse(
                ytelseType = Ytelse.SYKEPENGER,
                ytelsePerioder = setOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusYears(3)),
                        gradering = Prosent(66),
                    ),
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusYears(3).plusDays(2), LocalDate.now().plusYears(6)),
                        gradering = Prosent(31)
                    )
                ),
                kilde = "kilde",
                saksRef = "abc"
            ),
            SamordningYtelse(
                ytelseType = Ytelse.OMSORGSPENGER,
                ytelsePerioder = setOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusYears(2)),
                        gradering = Prosent(51),
                    ),
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusYears(3).plusDays(2), LocalDate.now().plusYears(6)),
                        gradering = Prosent(67)
                    )
                ),
                kilde = "kilde",
                saksRef = "abc"
            )
        )

        dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).lagre(
                behandling.id,
                samordningYtelser = samordningYtelser2
            )
        }

        val uthentet2 = dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }

        assertThat(uthentet2!!.ytelser.size).isEqualTo(2)
        assertThat(samordningYtelser2).containsExactlyInAnyOrderElementsOf(uthentet2.ytelser)

        // Kopier:
        val kopiertBehandling = dataSource.transaction {
            val nyBehandling = finnEllerOpprettBehandling(it, sak(it))

            SamordningYtelseRepositoryImpl(it).kopier(behandling.id, nyBehandling.id)
        }
    }

    @Test
    fun `sette inn flere ytelser, skal hente ut eldste`() {
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak(it)) }

        // Første sett med ytelser (eldste)
        val samordningYtelserEldste = setOf(
            SamordningYtelse(
                ytelseType = Ytelse.SYKEPENGER,
                ytelsePerioder = setOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
                        gradering = Prosent.`70_PROSENT`,
                    )
                ),
                kilde = "kilde-eldste",
                saksRef = "abc-eldste"
            )
        )
        dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).lagre(
                behandling.id,
                samordningYtelser = samordningYtelserEldste
            )
        }

        // Andre sett med ytelser (nyeste)
        val samordningYtelserNyeste = setOf(
            SamordningYtelse(
                ytelseType = Ytelse.OMSORGSPENGER,
                ytelsePerioder = setOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusYears(2)),
                        gradering = Prosent(51),
                    )
                ),
                kilde = "kilde-nyeste",
                saksRef = "abc-nyeste"
            )
        )
        dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).lagre(
                behandling.id,
                samordningYtelser = samordningYtelserNyeste
            )
        }

        // Verifiser at hentEldsteGrunnlag returnerer det eldste grunnlaget
        val eldsteGrunnlag = dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).hentEldsteGrunnlag(behandling.id)
        }
        assertThat(eldsteGrunnlag).isNotNull
        assertThat(eldsteGrunnlag!!.ytelser.size).isEqualTo(1)
        assertThat(eldsteGrunnlag.ytelser.first().kilde).isEqualTo("kilde-eldste")
        assertThat(eldsteGrunnlag.ytelser.first().saksRef).isEqualTo("abc-eldste")
        assertThat(eldsteGrunnlag.ytelser.first().ytelseType).isEqualTo(Ytelse.SYKEPENGER)

        // Verifiser at hentHvisEksisterer returnerer det nyeste grunnlaget
        val nyesteGrunnlag = dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(nyesteGrunnlag).isNotNull
        assertThat(nyesteGrunnlag!!.ytelser.size).isEqualTo(1)
        assertThat(nyesteGrunnlag.ytelser.first().kilde).isEqualTo("kilde-nyeste")
        assertThat(nyesteGrunnlag.ytelser.first().saksRef).isEqualTo("abc-nyeste")
        assertThat(nyesteGrunnlag.ytelser.first().ytelseType).isEqualTo(Ytelse.OMSORGSPENGER)
    }

    @Test
    fun `sette inn for flere behandlinger, hente ut`() {
        val behandling1 = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak(it))
        }
        val behandling2 = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak(it))
        }
        val samordningYtelser1 = setOf(
            SamordningYtelse(
                ytelseType = Ytelse.SYKEPENGER,
                ytelsePerioder = setOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
                        gradering = Prosent.`70_PROSENT`,
                    ),
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusYears(3).plusDays(1), LocalDate.now().plusYears(6)),
                        gradering = Prosent.`30_PROSENT`,
                    )
                ),
                kilde = "kilde",
                saksRef = "abc"
            )
        )
        val samordningYtelser2 = setOf(
            SamordningYtelse(
                ytelseType = Ytelse.SYKEPENGER,
                ytelsePerioder = setOf(
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(3)),
                        gradering = Prosent.`70_PROSENT`,
                    ),
                    SamordningYtelsePeriode(
                        periode = Periode(LocalDate.now().plusYears(3).plusDays(1), LocalDate.now().plusYears(6)),
                        gradering = Prosent.`100_PROSENT`,
                    )
                ),
                kilde = "kilde2",
                saksRef = "xxx"
            )
        )
        dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).lagre(
                behandling1.id,
                samordningYtelser = samordningYtelser1
            )
            SamordningYtelseRepositoryImpl(it).lagre(
                behandling2.id,
                samordningYtelser = samordningYtelser2
            )
        }

        val uthentet1 = dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).hentHvisEksisterer(behandling1.id)
        }
        assertThat(samordningYtelser1).isEqualTo(uthentet1!!.ytelser)
        val uthentet2 = dataSource.transaction {
            SamordningYtelseRepositoryImpl(it).hentHvisEksisterer(behandling2.id)
        }
        assertThat(samordningYtelser2).isEqualTo(uthentet2!!.ytelser)

        // Kopier:
        val kopiertBehandling = dataSource.transaction {
            val nyBehandling = finnEllerOpprettBehandling(it, sak(it))

            SamordningYtelseRepositoryImpl(it).kopier(behandling1.id, nyBehandling.id)
        }
    }

    @Test
    fun `test sletting`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val samordningYtelseRepository = SamordningYtelseRepositoryImpl(connection)
            samordningYtelseRepository.lagre(
                behandling.id, setOf(
                    SamordningYtelse(
                        ytelseType = Ytelse.SYKEPENGER,
                        ytelsePerioder = setOf(
                            SamordningYtelsePeriode(
                                periode = Periode(
                                    fom = LocalDate.of(2023, 1, 1),
                                    tom = LocalDate.of(2023, 12, 31)
                                ),
                                gradering = null,
                                kronesum = 1000
                            )
                        ),
                        kilde = "TEST1",
                        saksRef = "REF1"
                    )
                )
            )
            samordningYtelseRepository.lagre(
                behandling.id, setOf(
                    SamordningYtelse(
                        ytelseType = Ytelse.SYKEPENGER,
                        ytelsePerioder = setOf(
                            SamordningYtelsePeriode(
                                periode = Periode(
                                    fom = LocalDate.of(2024, 1, 1),
                                    tom = LocalDate.of(2024, 12, 31)
                                ),
                                gradering = null,
                                kronesum = 1000
                            )
                        ),
                        kilde = "TEST1",
                        saksRef = "REF1"
                    )
                )
            )
            assertDoesNotThrow {
                samordningYtelseRepository.slett(behandling.id)
            }
        }
    }


    @Test
    fun `full overlapping med eksisterende ytelser med nye ytelser`() {

        val eksisterendeGrunnlag = SamordningYtelseGrunnlag(
            grunnlagId = 1L,
            ytelser = setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    kilde = "KildeA",
                    saksRef = "SAK123",
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = Periode(
                                1 januar 2023,
                                tom = 30 juni 2023
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            kronesum = 15000
                        ),
                        SamordningYtelsePeriode(
                            periode = Periode(
                                fom = 1 juli 2023,
                                tom = 31 desember 2023
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            kronesum = 30000
                        )
                    )
                )
            )
        )

        val nyYtelse = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            kilde = "KildeB",
            saksRef = null,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        1 mai 2023,
                        31 desember 2023
                    ),
                    gradering = Prosent.`100_PROSENT`,
                    kronesum = 25000
                )
            )
        )

        assertFalse(
            harEndringerIYtelserIkkeDekketAvEksisterendeGrunnlag(
                eksisterendeGrunnlag,
                setOf(nyYtelse)
            )
        )

    }

    @Test
    fun `delvis overlapping med eksisterende ytelser og nye ytelser`() {
        val eksisterendeGrunnlag = SamordningYtelseGrunnlag(
            grunnlagId = 1L,
            ytelser = setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    kilde = "KildeA",
                    saksRef = "SAK123",
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = Periode(
                                fom = 1 januar 2023,
                                tom = 30 juni 2023
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            kronesum = 15000
                        ),
                        SamordningYtelsePeriode(
                            periode = Periode(
                                1 juli 2023,
                                31 desember 2023
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            kronesum = 30000
                        )
                    )
                )
            )
        )

        val nyYtelse = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            kilde = "KildeB",
            saksRef = null,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        1 mai 2023,
                        30 september 2023
                    ),
                    gradering = Prosent.`100_PROSENT`,
                    kronesum = 25000
                )
            )
        )

        assertFalse(
            harEndringerIYtelserIkkeDekketAvEksisterendeGrunnlag(
                eksisterendeGrunnlag,
                setOf(nyYtelse)
            )
        )
    }

    @Test
    fun `delvis overlapping mellom eksisterende ytelser og nye ytelser inne i samme periode`() {
        val eksisterendeGrunnlag = SamordningYtelseGrunnlag(
            grunnlagId = 1L,
            ytelser = setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    kilde = "KildeA",
                    saksRef = "SAK123",
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = Periode(
                                1 januar 2023,
                                30 juni 2023,
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            kronesum = 15000
                        ),
                    )
                )
            )
        )

        val nyYtelse = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            kilde = "KildeB",
            saksRef = null,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        1 januar 2023,
                        30 september 2024,
                    ),
                    gradering = Prosent.`100_PROSENT`,
                    kronesum = 25000
                )
            )
        )

        assertTrue(
            harEndringerIYtelserIkkeDekketAvEksisterendeGrunnlag(
                eksisterendeGrunnlag,
                setOf(nyYtelse)
            )
        )
    }

    @Test
    fun `ikke overlapping mellom eksisterende ytelser og nye ytelser`() {
        val eksisterendeGrunnlag = SamordningYtelseGrunnlag(
            grunnlagId = 1L,
            ytelser = setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    kilde = "KildeA",
                    saksRef = "SAK123",
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = Periode(
                                1 januar 2023,
                                30 juni 2023,
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            kronesum = 15000
                        ),
                    )
                )
            )
        )

        val nyYtelse = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            kilde = "KildeB",
            saksRef = null,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        1 mai 2024,
                        30 september 2024,
                    ),
                    gradering = Prosent.`100_PROSENT`,
                    kronesum = 25000
                )
            )
        )

        assertTrue(
            harEndringerIYtelserIkkeDekketAvEksisterendeGrunnlag(
                eksisterendeGrunnlag,
                setOf(nyYtelse)
            )
        )
    }


    @Test
    fun `ingen overlapp med eksisterende ytelser når den nye ytelsen har annen ytelsestype`() {
        val eksisterendeGrunnlag = SamordningYtelseGrunnlag(
            grunnlagId = 1L,
            ytelser = setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    kilde = "KildeA",
                    saksRef = "SAK123",
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = Periode(
                                1 mai 2023,
                                tom = 31 desember 2023
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            kronesum = 15000
                        ),
                    )
                )
            )
        )

        val nyYtelse = SamordningYtelse(
            ytelseType = Ytelse.FORELDREPENGER,
            kilde = "KildeB",
            saksRef = null,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        1 mai 2023,
                        31 desember 2023
                    ),
                    gradering = Prosent.`100_PROSENT`,
                    kronesum = 25000
                )
            )
        )

        assertTrue(
            harEndringerIYtelserIkkeDekketAvEksisterendeGrunnlag(
                eksisterendeGrunnlag,
                setOf(nyYtelse)
            )
        )
    }

    @Test
    fun `ignorerer nye opplysninger når gradering er 100 prosent og ytelse er sykeoenger`() {
        val eksisterendeGrunnlag = SamordningYtelseGrunnlag(
            grunnlagId = 1L,
            ytelser = setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    kilde = "KildeA",
                    saksRef = "SAK123",
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = Periode(
                                1 mai 2023,
                                tom = 31 mai 2023
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            kronesum = 15000
                        ),
                        SamordningYtelsePeriode(
                            periode = Periode(
                                1 juni 2023,
                                tom = 30 juni 2023
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            kronesum = 15000
                        ),
                    )
                )
            )
        )

        val nyYtelse = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            kilde = "KildeB",
            saksRef = null,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        1 mai 2023,
                        30 juni 2023
                    ),
                    gradering = Prosent(40),
                    kronesum = 25000
                )
            )
        )

        assertFalse(
            harEndringerIYtelserIkkeDekketAvEksisterendeGrunnlag(
                eksisterendeGrunnlag,
                setOf(nyYtelse)
            )
        )
    }

    @Test
    fun `ønsker å lagre nye opplysninger når gradering er 100 prosent og ytelse er foreldrepenger`() {
        val eksisterendeGrunnlag = SamordningYtelseGrunnlag(
            grunnlagId = 1L,
            ytelser = setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.FORELDREPENGER,
                    kilde = "KildeA",
                    saksRef = "SAK123",
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = Periode(
                                1 mai 2023,
                                tom = 31 mai 2023
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            kronesum = 15000
                        ),
                        SamordningYtelsePeriode(
                            periode = Periode(
                                1 juni 2023,
                                tom = 30 juni 2023
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            kronesum = 15000
                        ),
                    )
                )
            )
        )

        val nyYtelse = SamordningYtelse(
            ytelseType = Ytelse.FORELDREPENGER,
            kilde = "KildeB",
            saksRef = null,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        1 mai 2023,
                        30 juni 2023
                    ),
                    gradering = Prosent(40),
                    kronesum = 25000
                )
            )
        )

        assertTrue(
            harEndringerIYtelserIkkeDekketAvEksisterendeGrunnlag(
                eksisterendeGrunnlag,
                setOf(nyYtelse)
            )
        )
    }

    @Test
    fun `ingen endringer i ytelser om vi har manuelle vurderinger fra før `() {
        val vurderingsGrunnlag = SamordningVurderingGrunnlag(
            begrunnelse = "",
            vurderinger = setOf(
                SamordningVurdering(
                    Ytelse.SYKEPENGER, setOf(
                        SamordningVurderingPeriode(
                            periode = Periode(
                                1 mai 2023,
                                31 desember 2023
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            manuell = false,
                        )
                    )
                )
            ),
            vurdertAv = "ident"
        )

        val nyYtelse = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            kilde = "KildeB",
            saksRef = null,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        1 mai 2023,
                        31 desember 2023
                    ),
                    gradering = Prosent.`100_PROSENT`,
                    kronesum = 25000
                )
            )
        )

        assertFalse(harEndringerIYtelserIkkeDekketAvManuelleVurderinger(vurderingsGrunnlag, setOf(nyYtelse)))
    }

    @Test
    fun `test endringer i ytelser som vi ikke har manuelle vurderinger for`() {
        val vurderingsGrunnlag = SamordningVurderingGrunnlag(
            begrunnelse = "",
            vurderinger = setOf(
                SamordningVurdering(
                    Ytelse.SYKEPENGER, setOf(
                        SamordningVurderingPeriode(
                            periode = Periode(
                                1 mai 2023,
                                31 oktober 2023
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            manuell = false,
                        )
                    )
                )
            ),
            vurdertAv = "ident"
        )

        val nyYtelse = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            kilde = "KildeB",
            saksRef = null,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        1 mai 2023,
                        31 desember 2023
                    ),
                    gradering = Prosent.`100_PROSENT`,
                    kronesum = 25000
                )
            )
        )

        assertTrue(harEndringerIYtelserIkkeDekketAvManuelleVurderinger(vurderingsGrunnlag, setOf(nyYtelse)))
    }

    @Test
    fun `test endringer i sykepenger som vi ikke har manuelle vurderinger for, men bare graderingen er endret`() {
        val vurderingsGrunnlag = SamordningVurderingGrunnlag(
            begrunnelse = "",
            vurderinger = setOf(
                SamordningVurdering(
                    Ytelse.SYKEPENGER, setOf(
                        SamordningVurderingPeriode(
                            periode = Periode(
                                1 mai 2023,
                                31 oktober 2023
                            ),
                            gradering = Prosent.`50_PROSENT`,
                            manuell = false,
                        )
                    )
                )
            ),
            vurdertAv = "ident"
        )

        val nyYtelse = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            kilde = "KildeB",
            saksRef = null,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        1 mai 2023,
                        31 oktober 2023
                    ),
                    gradering = Prosent.`70_PROSENT`,
                    kronesum = 25000
                )
            )
        )

        assertTrue(harEndringerIYtelserIkkeDekketAvManuelleVurderinger(vurderingsGrunnlag, setOf(nyYtelse)))
    }

    @Test
    fun `test endringer i foreldrepenger som vi ikke har manuelle vurderinger for, men bare graderingen er endret`() {
        val vurderingsGrunnlag = SamordningVurderingGrunnlag(
            begrunnelse = "",
            vurderinger = setOf(
                SamordningVurdering(
                    Ytelse.FORELDREPENGER, setOf(
                        SamordningVurderingPeriode(
                            periode = Periode(
                                1 mai 2023,
                                31 oktober 2023
                            ),
                            gradering = Prosent.`50_PROSENT`,
                            manuell = false,
                        )
                    )
                )
            ),
            vurdertAv = "ident"
        )

        val nyYtelse = SamordningYtelse(
            ytelseType = Ytelse.FORELDREPENGER,
            kilde = "KildeB",
            saksRef = null,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        1 mai 2023,
                        31 oktober 2023
                    ),
                    gradering = Prosent.`70_PROSENT`,
                    kronesum = 25000
                )
            )
        )

        assertTrue(harEndringerIYtelserIkkeDekketAvManuelleVurderinger(vurderingsGrunnlag, setOf(nyYtelse)))
    }

    @Test
    fun `test endringer i sykepenger som vi ikke har manuelle vurderinger for, men bare graderingen er endret, og vi har allerede 100 prosent`() {
        val vurderingsGrunnlag = SamordningVurderingGrunnlag(
            begrunnelse = "",
            vurderinger = setOf(
                SamordningVurdering(
                    Ytelse.SYKEPENGER, setOf(
                        SamordningVurderingPeriode(
                            periode = Periode(
                                1 mai 2023,
                                31 oktober 2023
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            manuell = false,
                        )
                    )
                )
            ),
            vurdertAv = "ident"
        )

        val nyYtelse = SamordningYtelse(
            ytelseType = Ytelse.SYKEPENGER,
            kilde = "KildeB",
            saksRef = null,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        1 mai 2023,
                        31 oktober 2023
                    ),
                    gradering = Prosent.`70_PROSENT`,
                    kronesum = 25000
                )
            )
        )

        assertFalse(harEndringerIYtelserIkkeDekketAvManuelleVurderinger(vurderingsGrunnlag, setOf(nyYtelse)))
    }

    @Test
    fun `test endringer i foreldrepenger som vi ikke har manuelle vurderinger for, men bare graderingen er endret, og vi har allerede 100 prosent`() {
        val vurderingsGrunnlag = SamordningVurderingGrunnlag(
            begrunnelse = "",
            vurderinger = setOf(
                SamordningVurdering(
                    Ytelse.FORELDREPENGER, setOf(
                        SamordningVurderingPeriode(
                            periode = Periode(
                                1 mai 2023,
                                31 oktober 2023
                            ),
                            gradering = Prosent.`100_PROSENT`,
                            manuell = false,
                        )
                    )
                )
            ),
            vurdertAv = "ident"
        )

        val nyYtelse = SamordningYtelse(
            ytelseType = Ytelse.FORELDREPENGER,
            kilde = "KildeB",
            saksRef = null,
            ytelsePerioder = setOf(
                SamordningYtelsePeriode(
                    periode = Periode(
                        1 mai 2023,
                        31 oktober 2023
                    ),
                    gradering = Prosent.`50_PROSENT`,
                    kronesum = 25000
                )
            )
        )

        assertTrue(harEndringerIYtelserIkkeDekketAvManuelleVurderinger(vurderingsGrunnlag, setOf(nyYtelse)))
    }
}
