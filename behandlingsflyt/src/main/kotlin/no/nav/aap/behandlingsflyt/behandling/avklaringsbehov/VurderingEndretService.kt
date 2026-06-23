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
        return when (avklaringsbehov.definisjon) {
            Definisjon.SKRIV_SYKDOMSVURDERING_BREV -> {
                sykdomsvurderingForBrevRepository.hentAktivPåTidspunkt(behandlingId, tidspunkt)
                sykdomsvurderingForBrevRepository.hent(behandlingId)
                // TODO sjekk diff
                null
            }

            else -> null
        }
    }
}