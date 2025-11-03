package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarManuellInntektVurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Year

class AvklarManuellInntektVurderingLøserTest {
    @Test
    fun `kan ikke sende inn negativ manuell inntekt`() {
        val behandlingId = BehandlingId(1L)
        val beregningService = mockk<BeregningService>()
        every { beregningService.utledRelevanteBeregningsÅr(behandlingId) } returns setOf(Year.of(2022))
        val manuellInntektGrunnlagRepository = mockk<ManuellInntektGrunnlagRepository>()

        val løser = AvklarManuellInntektVurderingLøser(
            manuellInntektGrunnlagRepository = manuellInntektGrunnlagRepository,
            beregningService = beregningService,
            unleashGateway = FakeUnleash
        )

        val kontekst = AvklaringsbehovKontekst(
            bruker = Bruker("bruker"), kontekst = FlytKontekst(
                sakId = SakId(1L),
                behandlingId = behandlingId,
                forrigeBehandlingId = null,
                behandlingType = TypeBehandling.Revurdering
            )
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(
                kontekst,
                AvklarManuellInntektVurderingLøsning(
                    manuellVurderingForManglendeInntekt = ManuellInntektVurderingDto(
                        begrunnelse = "Mangler ligning",
                        belop = BigDecimal(-1)
                    )
                )
            )
        }
    }

    @Test
    fun `kan sende inn flere vurderinger samtidig`() {

    }
}