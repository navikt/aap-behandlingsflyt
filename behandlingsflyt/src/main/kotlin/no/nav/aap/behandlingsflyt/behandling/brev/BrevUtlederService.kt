package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.lookup.repository.RepositoryProvider

class BrevUtlederService(
    private val behandlingRepository: BehandlingRepository,
    private val resultatUtleder: ResultatUtleder,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        resultatUtleder = ResultatUtleder(repositoryProvider),
    )

    fun utledBehovForMeldingOmVedtak(behandlingId: BehandlingId): BrevBehov {
        val behandling = behandlingRepository.hent(behandlingId)

        when (behandling.typeBehandling()) {
            TypeBehandling.Førstegangsbehandling -> {
                val resultat = resultatUtleder.utledResultat(behandlingId)

                return when (resultat) {
                    Resultat.INNVILGELSE -> BrevBehov(TypeBrev.VEDTAK_INNVILGELSE)
                    Resultat.AVSLAG -> BrevBehov(TypeBrev.VEDTAK_AVSLAG)
                }
            }

            TypeBehandling.Revurdering -> {
                val årsakerTilBehandling = behandling.årsaker().map { it.type }.distinct()
                if (årsakerTilBehandling.size == 1 &&
                    årsakerTilBehandling.contains(ÅrsakTilBehandling.MOTTATT_MELDEKORT)
                ) {
                    return BrevBehov(null)
                }
                return BrevBehov(TypeBrev.VEDTAK_ENDRING)
            }

            TypeBehandling.Tilbakekreving, TypeBehandling.Klage ->
                return BrevBehov(null) // TODO
        }
    }
}
