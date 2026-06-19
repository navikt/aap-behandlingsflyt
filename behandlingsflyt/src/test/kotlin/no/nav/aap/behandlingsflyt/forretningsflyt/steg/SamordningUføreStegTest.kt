package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarSamordningUføreLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningUføreLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.help.avklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningUføreRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUføreRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SamordningUføreStegTest {

    @Test
    fun `skal kreve vurdering`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(1 juni 2026)

        InMemoryUføreRepository.lagre(
            behandling.id, setOf(
                Uføre(
                    virkningstidspunkt = 1 januar 2026,
                    uføregrad = Prosent(50),
                    uføregradFom = 1 januar 2026,
                    uføregradTom = null
                )
            )
        )
        kjørSteg(behandling)

        assertThat(hentAvklaringsbehov(behandling)?.erÅpent()).isTrue

        løsBehov(
            behandling, listOf(
                SamordningUføreVurderingPeriodeDto(
                    virkningstidspunkt = 1 januar 2026, uføregradTilSamordning = 50
                )
            )
        )

        kjørSteg(behandling)

        assertThat(hentAvklaringsbehov(behandling)?.erÅpent()).isFalse
    }

    @Test
    fun `skal ikke kreve vurdering av uførevedtak som slutter før rettighetsperiode start`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(1 juni 2026)

        InMemoryUføreRepository.lagre(
            behandling.id, setOf(
                Uføre(
                    virkningstidspunkt = 1 januar 2020,
                    uføregrad = Prosent(20),
                    uføregradFom = 1 januar 2020,
                    uføregradTom = 1 januar 2021
                ),
                Uføre(
                    virkningstidspunkt = 1 januar 2026,
                    uføregrad = Prosent(50),
                    uføregradFom = 1 januar 2026,
                    uføregradTom = null
                )
            )
        )
        kjørSteg(behandling)

        assertThat(hentAvklaringsbehov(behandling)?.erÅpent()).isTrue

        løsBehov(
            behandling, listOf(
                SamordningUføreVurderingPeriodeDto(
                    virkningstidspunkt = 1 januar 2026, uføregradTilSamordning = 50
                )
            )
        )

        kjørSteg(behandling)

        assertThat(hentAvklaringsbehov(behandling)?.erÅpent()).isFalse
    }

    private fun hentAvklaringsbehov(behandling: Behandling): Avklaringsbehov? =
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .hentBehovForDefinisjon(Definisjon.AVKLAR_SAMORDNING_UFØRE)

    private fun kjørSteg(
        behandling: Behandling
    ) {
        val steg = samordningUføreSteg()
        steg.utfør(flytKontekstMedPerioder { this.behandlingId = behandling.id })
    }

    private fun samordningUføreSteg(): SamordningUføreSteg = SamordningUføreSteg(
        InMemorySamordningUføreRepository,
        InMemoryUføreRepository,
        FakeTidligereVurderinger(),
        AvklaringsbehovService(inMemoryRepositoryProvider)
    )

    private fun løsBehov(
        behandling: Behandling,
        løsning: List<SamordningUføreVurderingPeriodeDto>
    ) {
        val behovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
        AvklarSamordningUføreLøser(inMemoryRepositoryProvider).løs(
            avklaringsbehovKontekst { this.behandling = behandling }, AvklarSamordningUføreLøsning(
                samordningUføreVurdering = SamordningUføreVurderingDto(
                    begrunnelse = "...", vurderingPerioder = løsning
                )
            )
        )
        behovene.løsAvklaringsbehov(
            Definisjon.AVKLAR_SAMORDNING_UFØRE,
            begrunnelse = "...",
            endretAv = "SAKSBEHANDLER",
        )
    }
}