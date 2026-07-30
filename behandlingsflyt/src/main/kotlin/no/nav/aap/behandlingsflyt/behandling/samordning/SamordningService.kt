package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

class SamordningService(
    private val samordningVurderingRepository: SamordningVurderingRepository,
    private val samordningYtelseRepository: SamordningYtelseRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningVurderingRepository = repositoryProvider.provide(),
        samordningYtelseRepository = repositoryProvider.provide(),
    )

    fun hentVurderinger(behandlingId: BehandlingId): SamordningVurderingGrunnlag? {
        return samordningVurderingRepository.hentHvisEksisterer(behandlingId)
    }

    fun hentYtelser(behandlingId: BehandlingId): SamordningYtelseGrunnlag? {
        return samordningYtelseRepository.hentHvisEksisterer(behandlingId)
    }

    fun samordningGrunnlag(behandlingId: BehandlingId): SamordningYtelseVurderingGrunnlag {
        return SamordningYtelseVurderingGrunnlag(
            hentYtelser(behandlingId),
            hentVurderinger(behandlingId)
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