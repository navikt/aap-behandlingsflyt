package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KvalitetssikringLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus

class KvalitetssikringFlytTest() : AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `Kvalitetssikringssteg - retur fra kvalitetssikrer`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling
            .løsSykdom(fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(alleAvklaringsbehov.filter { behov -> behov.erTotrinn() || behov.kreverKvalitetssikring() }
                .map { behov ->
                    TotrinnsVurdering(
                        behov.definisjon.kode, false, "begrunnelse", emptyList()
                    )
                }),
            Bruker("KVALITETSSIKRER"),
        )

        val avklaringsbehovSomKreverKvalitetssikring = hentAlleAvklaringsbehov(behandling)
            .filter { behov -> behov.kreverKvalitetssikring() }
        assertThat(avklaringsbehovSomKreverKvalitetssikring.any { it.status() == AvklaringsbehovStatus.SENDT_TILBAKE_FRA_KVALITETSSIKRER }).isTrue()
    }

    @Test
    fun `Kvalitetssikringssteg - retur fra beslutter`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )

        behandling
            .løsSykdom(fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap(fom)
            .løsOppholdskrav(fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())

        // Returner fra beslutter - ikke godkjenn avklar sykdom
        løsFatteVedtak(behandling, returVed = Definisjon.AVKLAR_SYKDOM)

        behandling
            .løsSykdom(fom)

        motor.kjørJobber()
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov.firstOrNull{it.definisjon == Definisjon.KVALITETSSIKRING}).isNotNull()

        behandling.kvalitetssikreOk()
            .foreslåVedtak()
        løsFatteVedtak(behandling)
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.SKRIV_VEDTAKSBREV)
    }

    @Test
    fun `Kvalitetssikringssteg - beslutter underkjenner steg før sykdom`() {
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
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap(fom)
            .løsOppholdskrav(fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())

        løsFatteVedtak(behandling, returVed = Definisjon.VURDER_RETTIGHETSPERIODE)

        behandling.løsRettighetsperiodeIngenEndring()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.any { it.definisjon == Definisjon.FATTE_VEDTAK }).isTrue()
    }


}