package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningYtelseRepository
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemorySakOgBehandlingService
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SamordningPeriodeSammenlignerTest {


    @Test
    fun `om det kun er en lagring, så er alle nye`() {
        val behandling = opprettBehandling(nySak())
        val samordningPeriodeSammenligner = SamordningPeriodeSammenligner(InMemorySamordningYtelseRepository)

        InMemorySamordningYtelseRepository.lagre(
            behandling.id, listOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = listOf(
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
        val behandling = opprettBehandling(nySak())
        val samordningPeriodeSammenligner = SamordningPeriodeSammenligner(InMemorySamordningYtelseRepository)

        val fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

        val førstePeriode = Periode(LocalDate.now(fixedClock), LocalDate.now(fixedClock).plusYears(1))


        val kilde = "xxx"
        val saksRef = "yyy"
        InMemorySamordningYtelseRepository.lagre(
            behandling.id, listOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = listOf(
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
            behandling.id, listOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = listOf(
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
        val behandling = opprettBehandling(nySak())
        val samordningPeriodeSammenligner = SamordningPeriodeSammenligner(InMemorySamordningYtelseRepository)

        val fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

        val førstePeriode = Periode(LocalDate.now(fixedClock), LocalDate.now(fixedClock).plusYears(1))


        val kilde = "xxx"
        val saksRef = "yyy"
        InMemorySamordningYtelseRepository.lagre(
            behandling.id, listOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = listOf(
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
            behandling.id, listOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = listOf(
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
        val behandling = opprettBehandling(nySak())
        val samordningPeriodeSammenligner = SamordningPeriodeSammenligner(InMemorySamordningYtelseRepository)

        val fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

        val førstePeriode = Periode(LocalDate.now(fixedClock), LocalDate.now(fixedClock).plusYears(1))

        val kilde = "xxx"
        val saksRef = "yyy"
        InMemorySamordningYtelseRepository.lagre(
            behandling.id, listOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = listOf(
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
            behandling.id, listOf(
                SamordningYtelse(
                    ytelseType = Ytelse.SYKEPENGER,
                    ytelsePerioder = listOf(
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
                    ytelsePerioder = listOf(
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

    private fun nySak(): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            InMemoryPersonRepository,
            InMemorySakRepository
        ).finnEllerOpprett(ident(), Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
    }
    private fun opprettBehandling(sak: Sak): Behandling {
        return InMemorySakOgBehandlingService
            .finnEllerOpprettBehandling(sak.saksnummer, listOf(Årsak(ÅrsakTilBehandling.MOTTATT_SØKNAD)))
            .behandling
    }
}