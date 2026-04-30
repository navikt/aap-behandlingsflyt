package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningYtelseRepository
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SamordningPeriodeSammenlignerTest {

    private val samordningPeriodeSammenligner = SamordningPeriodeSammenligner(InMemorySamordningYtelseRepository)

    @Test
    fun `om det kun er en lagring, så er alle nye`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        InMemorySamordningYtelseRepository.lagre(
            behandling.id, setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1)),
                            gradering = Prosent.`66_PROSENT`,
                        )
                    ),
                    kilde = "xxx",
                    saksRef = "yyy"
                )
            )
        )

        val res = samordningPeriodeSammenligner.hentPerioderMarkertMedEndringer(behandlingId = behandling.id)

        assertThat(res).hasSize(1)
        assertThat(res).allMatch { it.endringStatus == EndringStatus.NY }
    }

    @Test
    fun `det kommer en ekstra, ikke overlappende periode`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        val fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

        val førstePeriode = Periode(LocalDate.now(fixedClock), LocalDate.now(fixedClock).plusYears(1))


        val kilde = "xxx"
        val saksRef = "yyy"
        InMemorySamordningYtelseRepository.lagre(
            behandling.id, setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = førstePeriode,
                            gradering = Prosent.`66_PROSENT`,
                        ),
                    ),
                    kilde = kilde,
                    saksRef = saksRef
                )
            )
        )

        val andrePeriode = Periode(
            LocalDate.now(fixedClock).plusYears(1).plusDays(1),
            LocalDate.now(fixedClock).plusYears(2)
        )
        InMemorySamordningYtelseRepository.lagre(
            behandling.id, setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = førstePeriode,
                            gradering = Prosent.`66_PROSENT`,
                        ), SamordningYtelsePeriode(
                            periode = andrePeriode,
                            gradering = Prosent.`50_PROSENT`,
                        )
                    ),
                    kilde = kilde,
                    saksRef = saksRef
                )
            )
        )

        val res = samordningPeriodeSammenligner.hentPerioderMarkertMedEndringer(behandlingId = behandling.id)

        assertThat(res).hasSize(2)
        assertThat(res).containsExactlyInAnyOrder(
            SamordningYtelseMedEndring(
                ytelseType = Ytelse.SYKEPENGER,
                kilde = kilde,
                saksRef = saksRef,
                periode = førstePeriode,
                gradering = Prosent.`66_PROSENT`,
                endringStatus = EndringStatus.UENDRET
            ), SamordningYtelseMedEndring(
                ytelseType = Ytelse.SYKEPENGER,
                kilde = kilde,
                saksRef = saksRef,
                periode = andrePeriode,
                gradering = Prosent.`50_PROSENT`,
                endringStatus = EndringStatus.NY
            )
        )
    }

    @Test
    fun `periode har blitt forlenget, skal markeres som NY`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        val fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

        val førstePeriode = Periode(LocalDate.now(fixedClock), LocalDate.now(fixedClock).plusYears(1))


        val kilde = "xxx"
        val saksRef = "yyy"
        InMemorySamordningYtelseRepository.lagre(
            behandling.id, setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = førstePeriode,
                            gradering = Prosent.`66_PROSENT`,
                        ),
                    ),
                    kilde = kilde,
                    saksRef = saksRef
                )
            )
        )

        InMemorySamordningYtelseRepository.lagre(
            behandling.id, setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = førstePeriode.utvid(Periode(førstePeriode.fom, førstePeriode.tom.plusDays(10))),
                            gradering = Prosent.`66_PROSENT`,
                        ),
                    ),
                    kilde = kilde,
                    saksRef = saksRef
                )
            )
        )

        val res = samordningPeriodeSammenligner.hentPerioderMarkertMedEndringer(behandlingId = behandling.id)

        assertThat(res).hasSize(2)
        assertThat(res).containsExactlyInAnyOrder(
            SamordningYtelseMedEndring(
                ytelseType = Ytelse.SYKEPENGER,
                kilde = kilde,
                saksRef = saksRef,
                periode = førstePeriode,
                gradering = Prosent.`66_PROSENT`,
                endringStatus = EndringStatus.SLETTET
            ), SamordningYtelseMedEndring(
                ytelseType = Ytelse.SYKEPENGER,
                kilde = kilde,
                saksRef = saksRef,
                periode = førstePeriode.utvid(Periode(førstePeriode.fom, førstePeriode.tom.plusDays(10))),
                gradering = Prosent.`66_PROSENT`,
                endringStatus = EndringStatus.NY
            )
        )
    }

    @Test
    fun `ny periode med annen ytelse skal ikke endre status`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        val fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

        val førstePeriode = Periode(LocalDate.now(fixedClock), LocalDate.now(fixedClock).plusYears(1))

        val kilde = "xxx"
        val saksRef = "yyy"
        InMemorySamordningYtelseRepository.lagre(
            behandling.id, setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = førstePeriode,
                            gradering = Prosent.`66_PROSENT`,
                        ),
                    ),
                    kilde = kilde,
                    saksRef = saksRef
                )
            )
        )

        InMemorySamordningYtelseRepository.lagre(
            behandling.id, setOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = førstePeriode,
                            gradering = Prosent.`66_PROSENT`,
                        ),
                    ),
                    kilde = kilde,
                    saksRef = saksRef
                ),
                SamordningYtelse(
                    ytelseType = Ytelse.OMSORGSPENGER,
                    ytelsePerioder = setOf(
                        SamordningYtelsePeriode(
                            periode = førstePeriode,
                            gradering = Prosent.`30_PROSENT`
                        ),
                    ),
                    kilde = kilde,
                    saksRef = saksRef
                ),
            )
        )

        val res = samordningPeriodeSammenligner.hentPerioderMarkertMedEndringer(behandlingId = behandling.id)

        assertThat(res).hasSize(2)
        assertThat(res).containsExactlyInAnyOrder(
            SamordningYtelseMedEndring(
                ytelseType = Ytelse.SYKEPENGER,
                kilde = kilde,
                saksRef = saksRef,
                periode = førstePeriode,
                gradering = Prosent.`66_PROSENT`,
                endringStatus = EndringStatus.UENDRET
            ), SamordningYtelseMedEndring(
                ytelseType = Ytelse.OMSORGSPENGER,
                kilde = kilde,
                saksRef = saksRef,
                periode = førstePeriode,
                gradering = Prosent.`30_PROSENT`,
                endringStatus = EndringStatus.NY
            )
        )
    }

}