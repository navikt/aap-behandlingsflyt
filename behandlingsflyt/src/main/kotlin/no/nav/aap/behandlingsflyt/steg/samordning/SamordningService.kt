package no.nav.aap.behandlingsflyt.steg.samordning

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.steg.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.steg.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.samordning.SamordningYtelseVurderingGrunnlag

class SamordningService(
    private val samordningVurderingRepository: SamordningVurderingRepository,
    private val samordningYtelseRepository: SamordningYtelseRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningVurderingRepository = repositoryProvider.provide(),
        samordningYtelseRepository = repositoryProvider.provide(),
    )

    fun samordningGrunnlag(behandlingId: BehandlingId): SamordningYtelseVurderingGrunnlag {
        return SamordningYtelseVurderingGrunnlag(
            samordningYtelseRepository.hentHvisEksisterer(behandlingId),
            samordningVurderingRepository.hentHvisEksisterer(behandlingId)
        )
    }

    fun tilbakestillVurderinger(behandlingId: BehandlingId, forrigeBehandlingId: BehandlingId?) {
        val vurderinger = samordningVurderingRepository.hentHvisEksisterer(behandlingId)
        val forrigeVurderinger =
            forrigeBehandlingId?.let { samordningVurderingRepository.hentHvisEksisterer(it) }

        if (forrigeVurderinger != vurderinger) {
            if (forrigeBehandlingId == null || forrigeVurderinger == null) {
                // Er ingen forrige behandlingId, så vi deaktiverer det eksisterende grunnlaget.
                samordningVurderingRepository.deaktiverGrunnlag(behandlingId)
            } else {
                samordningVurderingRepository.lagreVurderinger(
                    behandlingId, forrigeVurderinger
                )
            }
        }
    }
}