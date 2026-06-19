package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningUføreLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriodeDto
import no.nav.aap.behandlingsflyt.help.avklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningUføreRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUføreRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AvklarSamordningUføreLøserTest {
    @Test
    fun `Skal kaste feil dersom man sender inn to vurderinger for samme virkningstidspunkt`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        val samordningUføreLøser =
            AvklarSamordningUføreLøser(InMemorySamordningUføreRepository, InMemoryUføreRepository)
        val feil = assertThrows<UgyldigForespørselException> {
            samordningUføreLøser.løs(
                avklaringsbehovKontekst { this.behandling = behandling },
                løsning = AvklarSamordningUføreLøsning(
                    samordningUføreVurdering = SamordningUføreVurderingDto(
                        begrunnelse = "TODO()",
                        vurderingPerioder = listOf(
                            SamordningUføreVurderingPeriodeDto(
                                virkningstidspunkt = 1 januar 2020,
                                uføregradTilSamordning = 50
                            ),
                            SamordningUføreVurderingPeriodeDto(
                                virkningstidspunkt = 1 januar 2020,
                                uføregradTilSamordning = 50
                            ),
                        )
                    ),
                )
            )
        }

        assertThat(feil.message).contains("Det finnes duplikate vurderinger på samme virkningstidspunkt")
    }

    @Test
    fun `Skal ikke kaste feil dersom man sender inn to vurderinger for ulike virkningstidspunkt`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        val samordningUføreRepository = InMemorySamordningUføreRepository
        val samordningUføreLøser = AvklarSamordningUføreLøser(samordningUføreRepository, InMemoryUføreRepository)
        samordningUføreLøser.løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            løsning = AvklarSamordningUføreLøsning(
                samordningUføreVurdering = SamordningUføreVurderingDto(
                    begrunnelse = "TODO()",
                    vurderingPerioder = listOf(
                        SamordningUføreVurderingPeriodeDto(
                            virkningstidspunkt = 1 januar 2020,
                            uføregradTilSamordning = 50
                        ),
                        SamordningUføreVurderingPeriodeDto(
                            virkningstidspunkt = 1 januar 2021,
                            uføregradTilSamordning = 100
                        ),
                    )
                ),
            )
        )

        val lagredeVurderinger = samordningUføreRepository.hentHvisEksisterer(behandling.id)?.vurdering
        assertThat(lagredeVurderinger?.vurderingPerioder).hasSize(2)
    }


}

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