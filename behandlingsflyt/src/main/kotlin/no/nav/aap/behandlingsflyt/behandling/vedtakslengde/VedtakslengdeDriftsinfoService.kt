package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

class VedtakslengdeDriftsinfoService private constructor(
    private val vedtakslengdeRepository: VedtakslengdeRepository,
    private val sakRepository: SakRepository,
    private val virkningstidspunktService: VirkningstidspunktService
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vedtakslengdeRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        virkningstidspunktService = VirkningstidspunktService(repositoryProvider, gatewayProvider)
    )

    fun gjeldendeVurderinger(behandling: Behandling): Tidslinje<VedtakslengdeVurdering> {
        val grunnlag = vedtakslengdeRepository.hentHvisEksisterer(behandling.id)
            ?: return Tidslinje.empty()
        val startdato = virkningstidspunktService.finnVirkningstidspunkt(behandling)
            ?: sakRepository.hent(behandling.sakId).rettighetsperiode.fom

        return grunnlag.gjeldendeVurderinger(startdato)
    }
}
