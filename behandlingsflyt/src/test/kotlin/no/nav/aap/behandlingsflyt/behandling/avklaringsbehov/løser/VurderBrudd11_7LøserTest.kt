package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderBrudd11_7Løsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7LøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
class VurderBrudd11_7LøserTest {
    private val aktivitetsplikt11_7Repository = mockk<Aktivitetsplikt11_7Repository>()
    private val behandlingRepository = mockk<BehandlingRepository>()

    @ParameterizedTest
    @MethodSource("ugyldigeLøsninger")
    fun `Skal kaste ugyldig forespørsel-exception dersom innsendt løsning er ugyldig`(løsning: VurderBrudd11_7Løsning) {
        every { aktivitetsplikt11_7Repository.lagre(any(), any()) } returns Unit

        val løser = VurderBrudd11_7Løser(aktivitetsplikt11_7Repository, behandlingRepository)

        val exception = assertThrows<UgyldigForespørselException> {
            løser.løs(
                kontekst = lagAvklaringsvehovKontekst(),
                løsning = løsning
            )
        }

        assertThat(exception.message).isEqualTo("Utfallet skal være satt hvis, og bare hvis, aktivitetsplikten ikke er oppfylt")
    }

    @ParameterizedTest
    @MethodSource("gyldigeLøsninger")
    fun `Gydlig løsning skal validere`(løsning: VurderBrudd11_7Løsning) {
        every { aktivitetsplikt11_7Repository.lagre(any(), any()) } returns Unit

        val løser = VurderBrudd11_7Løser(aktivitetsplikt11_7Repository, behandlingRepository)

        val resultat = løser.løs(
            kontekst = lagAvklaringsvehovKontekst(),
            løsning = løsning
        )

        assertThat(resultat.begrunnelse).isEqualTo("Gyldig")
    }

    private fun lagAvklaringsvehovKontekst(): AvklaringsbehovKontekst =
        AvklaringsbehovKontekst(
            bruker = Bruker("12345678901"),
            kontekst = FlytKontekst(
                sakId = SakId(1L),
                behandlingId = BehandlingId(1L),
                forrigeBehandlingId = null,
                behandlingType = TypeBehandling.Aktivitetsplikt
            )
        )


    companion object {
        @JvmStatic
        private fun ugyldigeLøsninger() = listOf(
            VurderBrudd11_7Løsning(
                aktivitetsplikt11_7Vurdering = Aktivitetsplikt11_7LøsningDto(
                    begrunnelse = "Ikke oppfylt",
                    erOppfylt = false,
                    gjelderFra = LocalDate.now()
                )
            ),
            VurderBrudd11_7Løsning(
                aktivitetsplikt11_7Vurdering = Aktivitetsplikt11_7LøsningDto(
                    begrunnelse = "Oppfylt",
                    erOppfylt = true,
                    utfall = Utfall.OPPHØR,
                    gjelderFra = LocalDate.now()

                )
            )
        )

        @JvmStatic
        private fun gyldigeLøsninger() = listOf(
            VurderBrudd11_7Løsning(
                aktivitetsplikt11_7Vurdering = Aktivitetsplikt11_7LøsningDto(
                    begrunnelse = "Gyldig",
                    erOppfylt = false,
                    utfall = Utfall.OPPHØR,
                    gjelderFra = LocalDate.now()
                )
            ),
            VurderBrudd11_7Løsning(
                aktivitetsplikt11_7Vurdering = Aktivitetsplikt11_7LøsningDto(
                    begrunnelse = "Gyldig",
                    erOppfylt = true,
                    gjelderFra = LocalDate.now()
                )
            )
        )
    }
}