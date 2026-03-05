package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningBarnepensjonLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnepensjon.BarnepensjonLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnepensjon.BarnepensjonLøsningPeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonPeriode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.samordning.BarnepensjonRepositoryImpl
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import  no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus

class BarnepensjonFlytTest : AbstraktFlytOrkestratorTest(LokalUnleash::class) {

    @Test
    fun `Barnepensjon skal samordnes krone mot krone`() {
        val fom = LocalDate.now()
        val periode = Periode(fom, fom.plusYears(3))

        // Sender inn en søknad
        var (sak, behandling) = sendInnFørsteSøknad(
            periode = periode,
            søknad = SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            ),
        )

        sak.sendInnMeldekort(periode.dager().associateWith { 0.0 })
        // Legger til vurderingsbehov på førstegangsbehandlingen for å tvinge behov
        sak.opprettManuellRevurdering(listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_SAMORDNING_BARNEPENSJON))

        behandling
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(åpneAvklaringsbehov).isNotEmpty()
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsSykdom(periode.fom)
            .løsBistand(periode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .medKontekst {
                assertThat(
                    åpneAvklaringsbehov.map { it.definisjon }).containsExactly(Definisjon.SAMORDNING_BARNEPENSJON)
            }
            .løsAvklaringsBehov(
                AvklarSamordningBarnepensjonLøsning(
                    barnepensjonVurdering = BarnepensjonLøsningDto(
                        begrunnelse = "Mottar barnepensjon", perioder = listOf(
                            BarnepensjonLøsningPeriodeDto(
                                fom = YearMonth.of(2025, 1),
                                tom = YearMonth.of(2025, 4),
                                månedsbeløp = Beløp("10335.66")
                            ),
                            BarnepensjonLøsningPeriodeDto(
                                fom = YearMonth.of(2025, 5),
                                tom = YearMonth.of(2025, 10),
                                månedsbeløp = Beløp("10846.66")
                            )
                        )
                    ),
                )
            )
            .medKontekst {
                assertThat(
                    avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SAMORDNING_BARNEPENSJON)?.status()
                ).isEqualTo(
                    AvklaringsbehovStatus.AVSLUTTET
                )

                dataSource.transaction { connection ->
                    val barnepensjonRepository = BarnepensjonRepositoryImpl(connection)
                    assertThat(barnepensjonRepository.hentHvisEksisterer(this.behandling.id)?.vurdering?.perioder).containsExactly(
                        BarnepensjonPeriode(
                            fom = YearMonth.of(2025, 1),
                            tom = YearMonth.of(2025, 4),
                            månedsats = Beløp("10335.66")
                        ),
                        BarnepensjonPeriode(
                            fom = YearMonth.of(2025, 5),
                            tom = YearMonth.of(2025, 10),
                            månedsats = Beløp("10846.66")
                        )
                    )
                }
            }

        // TODO: Utvid test for å verifisere tilkjent ytelse
    }
}