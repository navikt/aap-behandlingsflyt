package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageNayLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNayLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
class VurderKlageNayLøserTest {
    @Test
    fun `løs skal returnere et løsningsresultat når vilkårSomOmgjøres inneholder gyldige hjemler`() {
        val klagebehandlingNayRepositoryMock = mockk<KlagebehandlingNayRepository>()
        every { klagebehandlingNayRepositoryMock.lagre(any(), any()) } returns Unit

        val vurderKlageNayLøser = VurderKlageNayLøser(klagebehandlingNayRepositoryMock)

        val løsning = vurderKlageNayLøser.løs(
            kontekst = lagAvklaringsbehovKontekst(),
            løsning = VurderKlageNayLøsning(
                klagevurderingNay = KlagevurderingNayLøsningDto(
                    begrunnelse = "Begrunnelse for klage",
                    notat = "Notat",
                    innstilling = KlageInnstilling.OMGJØR,
                    vilkårSomOpprettholdes = emptyList(),
                    vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_5)
                )
            )
        )

        assertThat(løsning.begrunnelse).isEqualTo("Begrunnelse for klage")
    }

    @Test
    fun `løs skal kaste exception når vilkårSomOmgjøres inneholder hjemler som ikke kan mappes via tilÅrsak`() {
        val klagebehandlingNayRepositoryMock = mockk<KlagebehandlingNayRepository>()
        every { klagebehandlingNayRepositoryMock.lagre(any(), any()) } returns Unit

        val vurderKlageNayLøser = VurderKlageNayLøser(klagebehandlingNayRepositoryMock)

        val løsning = VurderKlageNayLøsning(
            klagevurderingNay = KlagevurderingNayLøsningDto(
                begrunnelse = "Begrunnelse for klage",
                notat = "Notat",
                innstilling = KlageInnstilling.OMGJØR,
                vilkårSomOpprettholdes = emptyList(),
                vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_3)
            )
        )

        val exception = assertThrows<UgyldigForespørselException> {
            vurderKlageNayLøser.løs(
                kontekst = lagAvklaringsbehovKontekst(),
                løsning = løsning
            )
        }

        assertThat(exception.message).contains("FOLKETRYGDLOVEN_11_3")
    }

    private fun lagAvklaringsbehovKontekst(): AvklaringsbehovKontekst =
        AvklaringsbehovKontekst(
            bruker = Bruker("12345678901"),
            kontekst = FlytKontekst(
                sakId = SakId(1L),
                behandlingId = BehandlingId(1L),
                forrigeBehandlingId = null,
                behandlingType = TypeBehandling.Klage
            )
        )
}