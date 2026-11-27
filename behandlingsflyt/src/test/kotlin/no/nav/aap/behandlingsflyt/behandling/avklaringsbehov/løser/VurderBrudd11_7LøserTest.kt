package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderBrudd11_7Løsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7LøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class VurderBrudd11_7LøserTest {
    private val aktivitetsplikt11_7Repository = InMemoryAktivitetsplikt11_7Repository
    private val behandlingRepository = InMemoryBehandlingRepository
    private val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository

    @ParameterizedTest
    @MethodSource("ugyldigeLøsninger")
    fun `Skal kaste ugyldig forespørsel-exception dersom innsendt løsning er ugyldig`(løsning: VurderBrudd11_7Løsning) {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val løser = VurderBrudd11_7Løser(aktivitetsplikt11_7Repository, behandlingRepository, avklaringsbehovRepository)

        val exception = assertThrows<UgyldigForespørselException> {
            løser.løs(
                kontekst = lagAvklaringsvehovKontekst(behandling),
                løsning = løsning
            )
        }

        assertThat(exception.message).isEqualTo("Utfallet skal være satt hvis, og bare hvis, aktivitetsplikten ikke er oppfylt")
    }

    @ParameterizedTest
    @MethodSource("gyldigeLøsninger")
    fun `Gydlig løsning skal validere`(løsning: VurderBrudd11_7Løsning) {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        val løser = VurderBrudd11_7Løser(aktivitetsplikt11_7Repository, behandlingRepository, avklaringsbehovRepository)

        val resultat = løser.løs(
            kontekst = lagAvklaringsvehovKontekst(behandling),
            løsning = løsning
        )

        assertThat(resultat.begrunnelse).isEqualTo("Gyldig")
    }

    private fun lagAvklaringsvehovKontekst(behandling: Behandling): AvklaringsbehovKontekst =
        AvklaringsbehovKontekst(
            bruker = Bruker("12345678901"),
            kontekst = FlytKontekst(
                sakId = behandling.sakId,
                behandlingId = behandling.id,
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