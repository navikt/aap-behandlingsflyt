package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStansOpphørRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVedtakslengdeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

@Fakes
class StansOpphørServiceTest {

    private val service = StansOpphørService(InMemoryVedtakslengdeRepository, InMemoryStansOpphørRepository)

    @Test
    fun `skal filtrere bort stans-opphør som er for langt fram i tid`() {
        val behandling = nyBehandling()
        lagreVedtakslengde(behandling, sluttdato = LocalDate.now().plusYears(1))
        InMemoryStansOpphørRepository.lagre(
            behandling.id, StansOpphørGrunnlag(
                stansOgOpphør = setOf(stansEntry(behandling, LocalDate.now().plusYears(3), Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP))
            )
        )

        assertThat(service.vedtattStansOpphør(behandling.id)).isEmpty()
    }

    @Test
    fun `skal beholde stans-opphør som er innenfor vedtaksperioden`() {
        val behandling = nyBehandling()
        lagreVedtakslengde(behandling, sluttdato = LocalDate.now().plusYears(1))
        InMemoryStansOpphørRepository.lagre(
            behandling.id, StansOpphørGrunnlag(
                stansOgOpphør = setOf(
                    stansEntry(behandling, LocalDate.now().plusYears(3), Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP),
                    stansEntry(behandling, LocalDate.now().plusMonths(6), Avslagsårsak.BRUKER_OVER_67)
                )
            )
        )

        val resultat = service.vedtattStansOpphør(behandling.id)

        assertThat(resultat).isNotEmpty()
        assertThat(resultat.first().vurdering.årsaker).contains(Avslagsårsak.BRUKER_OVER_67)
    }

    @Test
    fun `gir tom liste når ingen grunnlag finnes`() {
        val behandling = nyBehandling()

        assertThat(service.vedtattStansOpphør(behandling.id)).isEmpty()
    }

    private fun nyBehandling(): Behandling {
        val sak = sak(inMemoryRepositoryProvider, LocalDate.now())
        return finnEllerOpprettBehandling(inMemoryRepositoryProvider, testGatewayProvider(), sak.saksnummer)
    }

    private fun lagreVedtakslengde(behandling: Behandling, sluttdato: LocalDate) {
        InMemoryVedtakslengdeRepository.lagre(
            behandling.id, listOf(
                VedtakslengdeVurdering(
                    sluttdato = sluttdato,
                    utvidetMed = ÅrMedHverdager.TREDJE_ÅR,
                    vurdertAv = Bruker("saksbehandler"),
                    vurdertIBehandling = behandling.id,
                    opprettet = Instant.now(),
                    begrunnelse = "..."
                )
            )
        )
    }

    private fun stansEntry(behandling: Behandling, fom: LocalDate, årsak: Avslagsårsak) =
        GjeldendeStansEllerOpphør(
            fom = fom,
            opprettet = Instant.now(),
            vurdertIBehandling = behandling.id,
            vurdering = Stans(årsaker = setOf(årsak))
        )

}