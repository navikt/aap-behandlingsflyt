package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageNayLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNayLøsningDto
import no.nav.aap.behandlingsflyt.help.avklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.ImMemoryKlagebehandlingNayRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VurderKlageNayLøserTest {

    @Test
    fun `løs skal returnere et løsningsresultat når vilkårSomOmgjøres inneholder gyldige hjemler`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        val vurderKlageNayLøser = VurderKlageNayLøser(ImMemoryKlagebehandlingNayRepository)

        val løsning = vurderKlageNayLøser.løs(
            kontekst = avklaringsbehovKontekst { this.behandling = behandling },
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
        val (_, behandling) = opprettInMemorySakOgBehandling()

        val vurderKlageNayLøser = VurderKlageNayLøser(ImMemoryKlagebehandlingNayRepository)

        val løsning = VurderKlageNayLøsning(
            klagevurderingNay = KlagevurderingNayLøsningDto(
                begrunnelse = "Begrunnelse for klage",
                notat = "Notat",
                innstilling = KlageInnstilling.OMGJØR,
                vilkårSomOpprettholdes = emptyList(),
                vilkårSomOmgjøres = listOf(Hjemmel.FOLKETRYGDLOVEN_11_31)
            )
        )

        val exception = assertThrows<UgyldigForespørselException> {
            vurderKlageNayLøser.løs(
                kontekst = avklaringsbehovKontekst { this.behandling = behandling },
                løsning = løsning
            )
        }

        assertThat(exception.message).contains("FOLKETRYGDLOVEN_11_3")
    }
}