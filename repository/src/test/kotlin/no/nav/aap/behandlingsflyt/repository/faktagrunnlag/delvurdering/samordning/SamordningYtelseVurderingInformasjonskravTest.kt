package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingInformasjonskrav.Companion.harEndringerIYtelserIkkeDekketAvEksisterendeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingInformasjonskrav.Companion.harEndringerIYtelserIkkeDekketAvManuelleVurderinger
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.oktober
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SamordningYtelseVurderingInformasjonskravTest {

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

        assertThat(
            harEndringerIYtelserIkkeDekketAvEksisterendeGrunnlag(
                eksisterendeGrunnlag,
                setOf(nyYtelse)
            )
        ).isTrue
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

        assertThat(
            harEndringerIYtelserIkkeDekketAvEksisterendeGrunnlag(
                eksisterendeGrunnlag,
                setOf(nyYtelse)
            )
        ).isTrue
    }

    @Test
    fun `har ikke endringer i ytelser når det ikke er nye ytelser`() {
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

        assertFalse(
            harEndringerIYtelserIkkeDekketAvEksisterendeGrunnlag(
                eksisterendeGrunnlag,
                emptySet()
            )
        )
    }

    @Test
    fun ` ingen overlapp med eksisterende ytelser når det ikke finnes ytelser fra før`() {
        val eksisterendeGrunnlag = SamordningYtelseGrunnlag(
            grunnlagId = 1L,
            ytelser = emptySet()
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

    @Test
    fun `er endringer når det ikke finnes vurderinger fra før`() {
        val vurderingsGrunnlag = SamordningVurderingGrunnlag(
            begrunnelse = "Test",
            vurderinger = emptySet(),
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

    @Test
    fun `er ingen endringer når det ikke er nye ytelser`() {
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

        assertFalse(harEndringerIYtelserIkkeDekketAvManuelleVurderinger(vurderingsGrunnlag, emptySet()))
    }
}