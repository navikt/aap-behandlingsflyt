package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus

class KvalitetssikringFlytTest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {
    @Test
    fun `Kvalitetssikrer godkjenner alle avklaringsbehov`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()

        val avklaringsbehovSomKreverKvalitetssikring = hentAlleAvklaringsbehov(behandling)
            .filter { behov -> behov.kreverKvalitetssikring() }
        assertThat(avklaringsbehovSomKreverKvalitetssikring.all { it.status() == AvklaringsbehovStatus.KVALITETSSIKRET }).isTrue()
    }

    @Test
    fun `Kvalitetssikrer underkjenner alle avklaringsbehov`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre(underkjennVurderinger = Definisjon.entries)

        val avklaringsbehovSomKreverKvalitetssikring = hentAlleAvklaringsbehov(behandling)
            .filter { behov -> behov.kreverKvalitetssikring() }
        assertThat(avklaringsbehovSomKreverKvalitetssikring.all { it.status() == AvklaringsbehovStatus.SENDT_TILBAKE_FRA_KVALITETSSIKRER }).isTrue()
    }

    @Test
    fun `Kvalitetssikrer underkjenner AVKLAR_SYKDOM, men godkjenner de andre avklaringsbehovene`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre(underkjennVurderinger = listOf(Definisjon.AVKLAR_SYKDOM))

        val avklaringsbehovSomKreverKvalitetssikring = hentAlleAvklaringsbehov(behandling)
            .filter { behov -> behov.kreverKvalitetssikring() }
        assertThat(avklaringsbehovSomKreverKvalitetssikring.any { it.definisjon == Definisjon.AVKLAR_SYKDOM && it.status() == AvklaringsbehovStatus.SENDT_TILBAKE_FRA_KVALITETSSIKRER }).isTrue()
        assertThat(
            avklaringsbehovSomKreverKvalitetssikring
                .filter { it.definisjon != Definisjon.AVKLAR_SYKDOM }
                .all { it.status() == AvklaringsbehovStatus.KVALITETSSIKRET }
        ).isTrue()

        val stegetsEgetBehov = hentAlleAvklaringsbehov(behandling)
            .filter { behov -> behov.definisjon == Definisjon.KVALITETSSIKRING }
        assertThat(stegetsEgetBehov.first().status()).isEqualTo(AvklaringsbehovStatus.OPPRETTET)
    }

    @Test
    fun `Kvalitetssikrer underkjenner AVKLAR_BISTANDSBEHOV, men tar ikke stilling til de andre avklaringsbehovene`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre(
                behovÅKvalitetssikre = listOf(Definisjon.AVKLAR_BISTANDSBEHOV),
                underkjennVurderinger = listOf(Definisjon.AVKLAR_BISTANDSBEHOV)
            )

        val avklaringsbehovSomKreverKvalitetssikring = hentAlleAvklaringsbehov(behandling)
            .filter { behov -> behov.kreverKvalitetssikring() }

        assertThat(avklaringsbehovSomKreverKvalitetssikring)
            .anyMatch { it.definisjon == Definisjon.AVKLAR_BISTANDSBEHOV && it.status() == AvklaringsbehovStatus.SENDT_TILBAKE_FRA_KVALITETSSIKRER }
        assertThat(avklaringsbehovSomKreverKvalitetssikring.filter { it.definisjon != Definisjon.AVKLAR_BISTANDSBEHOV })
            .allMatch { it.status() == AvklaringsbehovStatus.AVSLUTTET }
    }

    @Test
    fun `Kvalitetssikrer underkjenner AVKLAR_BISTANDSBEHOV, men godkjenner de andre avklaringsbehovene`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre(underkjennVurderinger = listOf(Definisjon.AVKLAR_BISTANDSBEHOV))

        val avklaringsbehovSomKreverKvalitetssikring = hentAlleAvklaringsbehov(behandling)
            .filter { behov -> behov.kreverKvalitetssikring() }
        assertThat(avklaringsbehovSomKreverKvalitetssikring)
            .anyMatch { it.definisjon == Definisjon.AVKLAR_BISTANDSBEHOV && it.status() == AvklaringsbehovStatus.SENDT_TILBAKE_FRA_KVALITETSSIKRER }

        assertThat(avklaringsbehovSomKreverKvalitetssikring.filter { it.definisjon != Definisjon.AVKLAR_BISTANDSBEHOV })
            .allMatch { it.status() == AvklaringsbehovStatus.KVALITETSSIKRET }
    }


    @Test
    fun `Kvalitetssikrer underkjenner SKRIV_SYKDOMSVURDERING_BREV, men godkjenner de andre avklaringsbehovene`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre(underkjennVurderinger = listOf(Definisjon.SKRIV_SYKDOMSVURDERING_BREV))

        val avklaringsbehovSomKreverKvalitetssikring = hentAlleAvklaringsbehov(behandling)
            .filter { behov -> behov.kreverKvalitetssikring() }
        assertThat(avklaringsbehovSomKreverKvalitetssikring)
            .anyMatch { it.definisjon == Definisjon.SKRIV_SYKDOMSVURDERING_BREV && it.status() == AvklaringsbehovStatus.SENDT_TILBAKE_FRA_KVALITETSSIKRER }
        assertThat(avklaringsbehovSomKreverKvalitetssikring.filter { it.definisjon != Definisjon.SKRIV_SYKDOMSVURDERING_BREV })
            .allMatch { it.status() == AvklaringsbehovStatus.KVALITETSSIKRET }

    }

    @Test
    fun `Beslutter underkjenner AVKLAR_SYKDOM, kvalitetssikrer godkjenner på nytt, beslutter fatter vedtak`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .beslutterGodkjennerIkke(underkjennVurderinger = listOf(Definisjon.AVKLAR_SYKDOM))
            .løsSykdom(fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov)
                    .anyMatch { it.definisjon == Definisjon.KVALITETSSIKRING }
            }
            .kvalitetssikre()
            .foreslåVedtak()
            .fattVedtak()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).hasSize(1)
                    .first().extracting("definisjon").isEqualTo(Definisjon.SKRIV_VEDTAKSBREV)
            }
    }

    @Test
    fun `Beslutter underkjenner kun VURDER_RETTIGHETSPERIODE, ingen ny kvalitetssikring, beslutter fatter vedtak`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )

        sak.opprettManuellRevurdering(vurderingsbehov = Vurderingsbehov.VURDER_RETTIGHETSPERIODE)

        behandling
            .løsRettighetsperiodeIngenEndring()
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .beslutterGodkjennerIkke(underkjennVurderinger = listOf(Definisjon.VURDER_RETTIGHETSPERIODE))
            // Ingen endring
            .løsRettighetsperiodeIngenEndring()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).hasSize(1)
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.SKRIV_VEDTAKSBREV)
            }
    }

    @Test
    fun `Ny kvalitetssikring skal skje dersom beslutter underkjenner VURDER_RETTIGHETSPERIODE og AVKLAR_SYKDOM`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )

        sak.opprettManuellRevurdering(vurderingsbehov = Vurderingsbehov.VURDER_RETTIGHETSPERIODE)

        behandling
            .løsRettighetsperiodeIngenEndring()
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .beslutterGodkjennerIkke(underkjennVurderinger = listOf(Definisjon.VURDER_RETTIGHETSPERIODE, Definisjon.AVKLAR_SYKDOM))
            .løsRettighetsperiodeIngenEndring()
            .løsSykdom(fom)

        val stegetsEgetBehov = hentAlleAvklaringsbehov(behandling)
            .filter { behov -> behov.definisjon == Definisjon.KVALITETSSIKRING }
        assertThat(stegetsEgetBehov.first().status()).isEqualTo(AvklaringsbehovStatus.OPPRETTET)
    }

    @Test
    fun `Ny kvalitetssikring skal skje dersom behandlingen blir dratt tilbake til 22-13 og ny startdato settes`() {
        if (gatewayProvider.provide<UnleashGateway>().isDisabled(BehandlingsflytFeature.KvalitetssikringVed2213)) {
            return
        }

        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()

        val oppdatertBehandling = sak.opprettManuellRevurdering(listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE))
        val nyStartdato = LocalDate.now().minusMonths(5)
        oppdatertBehandling
            .løsRettighetsperiode(nyStartdato)
            .løsSykdom(nyStartdato)
            .løsBistand(nyStartdato)
            .løsSykdomsvurderingBrev()

        val stegetsEgetBehov = hentAlleAvklaringsbehov(oppdatertBehandling)
            .filter { behov -> behov.definisjon == Definisjon.KVALITETSSIKRING }
        assertThat(stegetsEgetBehov.first().status()).isEqualTo(AvklaringsbehovStatus.OPPRETTET)
    }

    @Test
    fun `Ny kvalitetssikring skal IKKE skje dersom behandlingen blir dratt tilbake til 22-13 og ingen ny startdato settes`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()

        val oppdatertBehandling = sak.opprettManuellRevurdering(listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE))
        oppdatertBehandling
            .løsRettighetsperiodeIngenEndring()

        val stegetsEgetBehov = hentAlleAvklaringsbehov(oppdatertBehandling)
            .filter { behov -> behov.definisjon == Definisjon.KVALITETSSIKRING }
        assertThat(stegetsEgetBehov.first().status()).isEqualTo(AvklaringsbehovStatus.AVSLUTTET)

        val fastsettBeregningstidspunktBehov = hentAlleAvklaringsbehov(oppdatertBehandling)
            .filter { behov -> behov.definisjon == Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT }
        assertThat(fastsettBeregningstidspunktBehov.first().status()).isEqualTo(AvklaringsbehovStatus.OPPRETTET)
    }
}
