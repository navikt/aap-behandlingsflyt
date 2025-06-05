package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EffektuerAvvistPåFormkravLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.ForhåndsvarselKlageFormkrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.flate.EffektuerAvvistPåFormkravLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test


class EffektuerAvvistPåFormkravLøserTest {

    @Test
    fun `Skal ikke kunne avvise på formkrav før frist for svar på forhåndsvarsel er utløpt`() {
        val effektuerMock = mockk<EffektuerAvvistPåFormkravRepository>()
        val formkravMock = mockk<FormkravRepository>()

        val effektuerLøser = EffektuerAvvistPåFormkravLøser(formkravMock, effektuerMock)

        val datoVarslet = LocalDate.now()
        val frist = datoVarslet.plusWeeks(2)

        every { formkravMock.hentHvisEksisterer(any()) } returns FormkravGrunnlag(
            vurdering = FormkravVurdering(
                begrunnelse = "Test begrunnelse",
                erBrukerPart = true,
                erFristOverholdt = true,
                likevelBehandles = null,
                erKonkret = false,
                erSignert = false,
                vurdertAv = "Saksbehandler"
            )
        )

        every { effektuerMock.hentHvisEksisterer(any()) } returns
                EffektuerAvvistPåFormkravGrunnlag(
                    varsel = ForhåndsvarselKlageFormkrav(
                        datoVarslet = datoVarslet,
                        frist = frist,
                        referanse = BrevbestillingReferanse(UUID.randomUUID())
                    ),
                    vurdering = null
                )

        val løsning = EffektuerAvvistPåFormkravLøsning(
            EffektuerAvvistPåFormkravLøsningDto(skalEndeligAvvises = true),
        )

        val exception = assertThrows<UgyldigForespørselException> {
            effektuerLøser.løs(
                kontekst = lagAvklaringsvehovKontekst(),
                løsning = løsning
            )
        }
        assert(exception.message == "Kan ikke avvise på formkrav før fristen er utløpt")
    }

    @Test
    fun `Skal ikke kunne avbryte effektuering uten å først ha endret vurdering for formkrav til godkjent`() {
        val effektuerMock = mockk<EffektuerAvvistPåFormkravRepository>()
        val formkravMock = mockk<FormkravRepository>()

        val effektuerLøser = EffektuerAvvistPåFormkravLøser(formkravMock, effektuerMock)

        every { formkravMock.hentHvisEksisterer(any()) } returns FormkravGrunnlag(
            vurdering = FormkravVurdering(
                begrunnelse = "Test begrunnelse",
                erBrukerPart = true,
                erFristOverholdt = true,
                likevelBehandles = null,
                erKonkret = false,
                erSignert = false,
                vurdertAv = "Saksbehandler"
            )
        )

        val løsning = EffektuerAvvistPåFormkravLøsning(
            EffektuerAvvistPåFormkravLøsningDto(skalEndeligAvvises = false),
        )

        val exception = assertThrows<UgyldigForespørselException> {
            effektuerLøser.løs(
                kontekst = lagAvklaringsvehovKontekst(),
                løsning = løsning
            )
        }
        assert(exception.message == "Løsningen er ikke konsistent med formkravvurderingen")
    }

    @Test
    fun `Skal kunne avbryte effektuering før frist er utløpt`() {
        val effektuerMock = mockk<EffektuerAvvistPåFormkravRepository>(relaxed = true)
        val formkravMock = mockk<FormkravRepository>()

        val effektuerLøser = EffektuerAvvistPåFormkravLøser(formkravMock, effektuerMock)
        
        every { formkravMock.hentHvisEksisterer(any()) } returns FormkravGrunnlag(
            vurdering = FormkravVurdering(
                begrunnelse = "Test begrunnelse",
                erBrukerPart = true,
                erFristOverholdt = true,
                likevelBehandles = null,
                erKonkret = true,
                erSignert = true,
                vurdertAv = "Saksbehandler"
            )
        )

        val løsning = EffektuerAvvistPåFormkravLøsning(
            EffektuerAvvistPåFormkravLøsningDto(skalEndeligAvvises = false),
        )

        val resultat = effektuerLøser.løs(
            kontekst = lagAvklaringsvehovKontekst(),
            løsning = løsning
        )

        assertThat(resultat).isEqualTo(LøsningsResultat("Effektuer avvist på formkrav"))
    }

    @Test
    fun `Skal kunne effektuere etter at fristen er utløpt`() {
        val effektuerMock = mockk<EffektuerAvvistPåFormkravRepository>(relaxed = true)
        val formkravMock = mockk<FormkravRepository>()

        val effektuerLøser = EffektuerAvvistPåFormkravLøser(formkravMock, effektuerMock)
        
        every { formkravMock.hentHvisEksisterer(any()) } returns FormkravGrunnlag(
            vurdering = FormkravVurdering(
                begrunnelse = "Test begrunnelse",
                erBrukerPart = true,
                erFristOverholdt = false,
                likevelBehandles = null,
                erKonkret = false,
                erSignert = false,
                vurdertAv = "Saksbehandler"
            )
        )

        val løsning = EffektuerAvvistPåFormkravLøsning(
            EffektuerAvvistPåFormkravLøsningDto(skalEndeligAvvises = true),
        )

        val resultat =
            effektuerLøser.løs(
                kontekst = lagAvklaringsvehovKontekst(),
                løsning = løsning
            )

        assertThat(resultat).isEqualTo(LøsningsResultat("Effektuer avvist på formkrav"))
    }

}

private fun lagAvklaringsvehovKontekst(): AvklaringsbehovKontekst =
    AvklaringsbehovKontekst(
        bruker = Bruker("12345678901"),
        kontekst = FlytKontekst(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Klage
        )
    )
