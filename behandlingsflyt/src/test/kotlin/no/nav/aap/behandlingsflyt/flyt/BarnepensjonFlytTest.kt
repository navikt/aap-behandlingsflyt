package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningBarnepensjonLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import  no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus

class BarnepensjonFlytTest: AbstraktFlytOrkestratorTest(LokalUnleash::class) {

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
            .løsAvklaringsBehov(AvklarSamordningBarnepensjonLøsning())
            .medKontekst {
                assertThat(
                    avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SAMORDNING_BARNEPENSJON)?.status()
                ).isEqualTo(
                    AvklaringsbehovStatus.AVSLUTTET
                )
            }
        
        // TODO: Utvid test for å verifisere underveis og tilkjent ytelse
    }
}