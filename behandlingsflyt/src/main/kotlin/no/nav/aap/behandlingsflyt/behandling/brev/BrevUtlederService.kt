package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository

class BrevUtlederService(
    private val behandlingRepository: BehandlingRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
) {

    fun utledBrevbehov(behandlingId: BehandlingId): BrevBehov {
        // TODO: Sjekk på behandlingen og utled hva som har skjedd for å avgjøre om det skal sendes et brev
        val behandling = behandlingRepository.hent(behandlingId)

        if (behandling.typeBehandling() != TypeBehandling.Førstegangsbehandling) {
            return BrevBehov(null)
        }

        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)

        return if (vilkårsresultat.alle().all { it.harPerioderSomErOppfylt() }) {
            BrevBehov(TypeBrev.VEDTAK_INNVILGELSE)
        } else {
            BrevBehov(TypeBrev.VEDTAK_AVSLAG)
        }
    }
}