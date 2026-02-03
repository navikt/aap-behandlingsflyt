package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KvalitetssikringLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderRettighetsperiodeLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
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

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(alleAvklaringsbehov.filter { behov -> behov.erTotrinn() || behov.kreverKvalitetssikring() }
                .map { behov ->
                    TotrinnsVurdering(
                        behov.definisjon.kode, true, "begrunnelse", emptyList()
                    )
                }),
            Bruker("KVALITETSSIKRER"),
        )

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

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(alleAvklaringsbehov.filter { behov -> behov.erTotrinn() || behov.kreverKvalitetssikring() }
                .map { behov ->
                    TotrinnsVurdering(
                        behov.definisjon.kode,
                        if (behov.definisjon == Definisjon.AVKLAR_SYKDOM) false else true,
                        "begrunnelse",
                        emptyList()
                    )
                }),
            Bruker("KVALITETSSIKRER"),
        )

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

        val avklarBistandsbehov = hentAlleAvklaringsbehov(behandling)
            .filter { behov -> behov.definisjon == Definisjon.AVKLAR_BISTANDSBEHOV }
            .filter { behov -> behov.erTotrinn() || behov.kreverKvalitetssikring() }
        assertThat(avklarBistandsbehov).hasSize(1)
        løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(
                avklarBistandsbehov
                    .map { behov ->
                        TotrinnsVurdering(
                            behov.definisjon.kode,
                            false,
                            "begrunnelse",
                            emptyList()
                        )
                    }),
            Bruker("KVALITETSSIKRER"),
        )

        val avklaringsbehovSomKreverKvalitetssikring = hentAlleAvklaringsbehov(behandling)
            .filter { behov -> behov.kreverKvalitetssikring() }
        assertThat(avklaringsbehovSomKreverKvalitetssikring.any { it.definisjon == Definisjon.AVKLAR_BISTANDSBEHOV && it.status() == AvklaringsbehovStatus.SENDT_TILBAKE_FRA_KVALITETSSIKRER }).isTrue()
        assertThat(
            avklaringsbehovSomKreverKvalitetssikring
                .filter { it.definisjon != Definisjon.AVKLAR_BISTANDSBEHOV }
                .all { it.status() == AvklaringsbehovStatus.AVSLUTTET }
        ).isTrue()
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

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(alleAvklaringsbehov.filter { behov -> behov.erTotrinn() || behov.kreverKvalitetssikring() }
                .map { behov ->
                    TotrinnsVurdering(
                        behov.definisjon.kode,
                        if (behov.definisjon == Definisjon.AVKLAR_BISTANDSBEHOV) false else true,
                        "begrunnelse",
                        emptyList()
                    )
                }),
            Bruker("KVALITETSSIKRER"),
        )

        val avklaringsbehovSomKreverKvalitetssikring = hentAlleAvklaringsbehov(behandling)
            .filter { behov -> behov.kreverKvalitetssikring() }
        assertThat(avklaringsbehovSomKreverKvalitetssikring.any { it.definisjon == Definisjon.AVKLAR_BISTANDSBEHOV && it.status() == AvklaringsbehovStatus.SENDT_TILBAKE_FRA_KVALITETSSIKRER }).isTrue()
        assertThat(
            avklaringsbehovSomKreverKvalitetssikring
                .filter { it.definisjon != Definisjon.AVKLAR_BISTANDSBEHOV }
                .all { it.status() == AvklaringsbehovStatus.KVALITETSSIKRET }
        ).isTrue()
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

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(alleAvklaringsbehov.filter { behov -> behov.erTotrinn() || behov.kreverKvalitetssikring() }
                .map { behov ->
                    TotrinnsVurdering(
                        behov.definisjon.kode,
                        if (behov.definisjon == Definisjon.SKRIV_SYKDOMSVURDERING_BREV) false else true,
                        "begrunnelse",
                        emptyList()
                    )
                }),
            Bruker("KVALITETSSIKRER"),
        )

        val avklaringsbehovSomKreverKvalitetssikring = hentAlleAvklaringsbehov(behandling)
            .filter { behov -> behov.kreverKvalitetssikring() }
        assertThat(avklaringsbehovSomKreverKvalitetssikring.any { it.definisjon == Definisjon.SKRIV_SYKDOMSVURDERING_BREV && it.status() == AvklaringsbehovStatus.SENDT_TILBAKE_FRA_KVALITETSSIKRER }).isTrue()
        assertThat(
            avklaringsbehovSomKreverKvalitetssikring
                .filter { it.definisjon != Definisjon.SKRIV_SYKDOMSVURDERING_BREV }
                .all { it.status() == AvklaringsbehovStatus.KVALITETSSIKRET }
        ).isTrue()
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
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .beslutterGodkjennerIkke(returVed = Definisjon.AVKLAR_SYKDOM)
            .løsSykdom(fom)

        motor.kjørJobber()
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov.firstOrNull { it.definisjon == Definisjon.KVALITETSSIKRING }).isNotNull()

        behandling.kvalitetssikreOk()
            .foreslåVedtak()
            .fattVedtak()

        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.SKRIV_VEDTAKSBREV)
    }

    // AVKLARING_SYKDOM får AVSLUTTET som siste status her i kvalitetssikringssteget,
    // uten at avklaringsbehovet er vurdert på nytt.
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
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .beslutterGodkjennerIkke(returVed = Definisjon.VURDER_RETTIGHETSPERIODE)

        behandling.løsRettighetsperiodeIngenEndring()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).hasSize(1)
        assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.SKRIV_VEDTAKSBREV)
    }

    @Test
    fun `Ny kvalitetssikring skal skje dersom behandlingen blir dratt tilbake til 22-13 og ny startdato settes`() {
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
            .kvalitetssikreOk()

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
        //
    }
}