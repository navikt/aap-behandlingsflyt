package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
class AvklarSykdomLøserTest {

    private val behandlingMock = mockk<BehandlingRepository>()
    private val sykdomMock = mockk<SykdomRepository>(relaxed = true)
    private val yrkesskadeMock = mockk<YrkesskadeRepository>()

    @Test
    fun `Vurdering som overskriver flere segmenter skal kun lage ett nytt segment`() {
        every { behandlingMock.hent(BehandlingId(2L)) } returns mockk {
            every { id } returns BehandlingId(2L)
            every { forrigeBehandlingId } returns BehandlingId(1L)
            every { typeBehandling() } returns TypeBehandling.Revurdering
        }

        every { yrkesskadeMock.hentHvisEksisterer(any()) } returns null

        every { sykdomMock.hentHvisEksisterer(any()) } returns
                SykdomGrunnlag(
                    id = 1L, yrkesskadevurdering = null, sykdomsvurderinger = listOf(
                        sykdomsvurdering(vurderingenGjelderFra = 1 januar 2025),
                        sykdomsvurdering(
                            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
                            vurderingenGjelderFra = 1 februar 2025
                        )
                    )
                )

        val sykdomLøser = AvklarSykdomLøser(behandlingMock, sykdomMock, yrkesskadeMock)
        sykdomLøser.løs(
            lagAvklaringsbehovKontekst(), løsning = AvklarSykdomLøsning(
                sykdomsvurderinger = listOf(
                    SykdomsvurderingLøsningDto(
                        begrunnelse = "",
                        dokumenterBruktIVurdering = emptyList(),
                        harSkadeSykdomEllerLyte = false,
                        erSkadeSykdomEllerLyteVesentligdel = null,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = null,
                        erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                        erArbeidsevnenNedsatt = null,
                        yrkesskadeBegrunnelse = null,
                        vurderingenGjelderFra = 10 januar 2025,
                    )
                )
            )
        )

        verify {
            sykdomMock.lagre(any(), sykdomsvurderinger = withArg {
                assertThat(it.size).isEqualTo(2)
            })
        }
    }
}

private fun sykdomsvurdering(
    harSkadeSykdomEllerLyte: Boolean = true,
    erSkadeSykdomEllerLyteVesentligdel: Boolean = true,
    erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean = true,
    erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean? = true,
    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean = true,
    erArbeidsevnenNedsatt: Boolean = true,
    vurderingenGjelderFra: LocalDate? = null,
    opprettet: LocalDateTime = LocalDateTime.now()
) = Sykdomsvurdering(
    begrunnelse = "",
    dokumenterBruktIVurdering = emptyList(),
    harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
    erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
    erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
    erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
    erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
    yrkesskadeBegrunnelse = null,
    vurderingenGjelderFra = vurderingenGjelderFra,
    vurdertAv = Bruker("Z00000"),
    opprettet = opprettet.toInstant(ZoneOffset.UTC)
)

private fun lagAvklaringsbehovKontekst(): AvklaringsbehovKontekst =
    AvklaringsbehovKontekst(
        bruker = Bruker("12345678901"),
        kontekst = FlytKontekst(
            sakId = SakId(1L),
            behandlingId = BehandlingId(2L),
            forrigeBehandlingId = BehandlingId(1L),
            behandlingType = TypeBehandling.Revurdering
        )
    )