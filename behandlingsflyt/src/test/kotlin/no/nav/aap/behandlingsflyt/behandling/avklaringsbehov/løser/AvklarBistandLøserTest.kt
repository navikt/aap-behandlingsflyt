package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class AvklarBistandLøserTest {
    private val bistandRepository = mockk<BistandRepository>(relaxed = true)
    private val løser = AvklarBistandLøser(bistandRepository)

    @Test
    fun `rapporterer unike valideringsfeil med tilhørende perioder før lagring`() {
        val løsning = AvklarBistandsbehovLøsning(
            løsningerForPerioder = listOf(
                ugyldigLøsning(LocalDate.of(2020, 1, 1)),
                ugyldigLøsning(LocalDate.of(2020, 2, 1))
            )
        )

        val exception = assertThrows<UgyldigForespørselException> {
            løser.løs(lagKontekst(), løsning)
        }

        val feilmelding =
            "erBehovForAnnenOppfølging kan bare bli besvart hvis erBehovForAktivBehandling og erBehovForArbeidsrettetTiltak er besvart med nei"
        assertThat(exception.message).isEqualTo(
            "$feilmelding (perioder: 2020-01-01, 2020-02-01)"
        )
        verify(exactly = 0) { bistandRepository.lagre(any(), any()) }
    }

    private fun ugyldigLøsning(fom: LocalDate) = BistandLøsningDto(
        fom = fom,
        tom = null,
        begrunnelse = "Begrunnelse",
        erBehovForAktivBehandling = true,
        erBehovForArbeidsrettetTiltak = false,
        erBehovForAnnenOppfølging = false,
        overgangBegrunnelse = null,
        skalVurdereAapIOvergangTilArbeid = null
    )

    private fun lagKontekst() = AvklaringsbehovKontekst(
        bruker = Bruker("12345678901"),
        kontekst = FlytKontekst(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling
        )
    )
}
