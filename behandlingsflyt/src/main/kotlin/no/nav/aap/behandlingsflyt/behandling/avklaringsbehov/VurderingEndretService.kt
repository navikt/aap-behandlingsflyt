package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class VurderingEndretService(
    private val sykdomsvurderingForBrevRepository: SykdomsvurderingForBrevRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        sykdomsvurderingForBrevRepository = repositoryProvider.provide()
    )

    fun endretSidenTidspunkt(
        behandlingId: BehandlingId,
        avklaringsbehov: Avklaringsbehov,
        tidspunkt: LocalDateTime
    ): Boolean? {
        // TODO: tilsvarende for alle avklaringsbehov som skal kvalitetssikres
        return when (avklaringsbehov.definisjon) {
            Definisjon.SKRIV_SYKDOMSVURDERING_BREV -> {
                val aktivPåTidspunkt = sykdomsvurderingForBrevRepository.hentAktivPåTidspunkt(behandlingId, tidspunkt)
                val aktivVurderingNå = sykdomsvurderingForBrevRepository.hent(behandlingId)

                if ((aktivPåTidspunkt == null && aktivVurderingNå != null) || (aktivPåTidspunkt != null && aktivVurderingNå == null)) {
                    return true
                }
                if (aktivPåTidspunkt == null || aktivVurderingNå == null) {
                    // avklaringsbehov aldri vurdert
                    return null
                }
                !aktivVurderingNå.erFunksjoneltLik(aktivPåTidspunkt)
            }

            else -> null
        }
    }
}