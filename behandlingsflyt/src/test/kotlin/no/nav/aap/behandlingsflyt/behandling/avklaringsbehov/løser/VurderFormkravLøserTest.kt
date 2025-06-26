package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderFormkravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.flate.FormkravVurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.httpklient.auth.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

class VurderFormkravLøserTest {
    private val formkravRepositoryMock = mockk<FormkravRepository>()
    private val avklaringsbehovRepositoryMock = mockk<AvklaringsbehovRepository>()
    private val avklaringsbehoveneMock = mockk<Avklaringsbehovene>()
    
    @BeforeEach
    fun setup() {
        every { avklaringsbehoveneMock.hentBehovForDefinisjon(any<Definisjon>()) } returns null
        every { avklaringsbehovRepositoryMock.hentAvklaringsbehovene(any()) } returns avklaringsbehoveneMock
    }

    @Test
    fun `løs skal returnere et løsningsresultat om alle verdier er satt`() {

        every { formkravRepositoryMock.lagre(any(), any()) } returns Unit

        val vurderFormkravLøser = VurderFormkravLøser(formkravRepositoryMock, avklaringsbehovRepositoryMock)

        val løsning = vurderFormkravLøser.løs(
            kontekst = lagAvklaringsvehovKontekst(),
            løsning = VurderFormkravLøsning(
                formkravVurdering = FormkravVurderingLøsningDto(
                    begrunnelse = "Begrunnelse for klage",
                    erFristOverholdt = true,
                    likevelBehandles = true,
                    erBrukerPart = true,
                    erKonkret = true,
                    erSignert = true
                )
            )
        )

        assertThat(løsning.begrunnelse).isEqualTo("Begrunnelse for klage")
    }

    @Test
    fun `løs skal kaste exception om man har svart nei på om frist er overholdt og ikke svart på om den likevel skal behandles`() {
        every { formkravRepositoryMock.lagre(any(), any()) } returns Unit

        val vurderFormkravLøser = VurderFormkravLøser(formkravRepositoryMock, avklaringsbehovRepositoryMock)

        val løsning = VurderFormkravLøsning(
            formkravVurdering = FormkravVurderingLøsningDto(
                begrunnelse = "Begrunnelse for klage",
                erFristOverholdt = false,
                likevelBehandles = null,
                erBrukerPart = true,
                erKonkret = true,
                erSignert = true
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            vurderFormkravLøser.løs(
                kontekst = lagAvklaringsvehovKontekst(),
                løsning = løsning
            )
        }

        assertThat(exception.message).isEqualTo("likevelBehandles må være satt dersom frist ikke er overholdt")
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
}