package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository

class BrevUtlederService(
    private val behandlingRepository: BehandlingRepository,
    private val underveisRepository: UnderveisRepository,
) {

    fun utledBehovForMeldingOmVedtak(behandlingId: BehandlingId): BrevBehov {
        val behandling = behandlingRepository.hent(behandlingId)

        if (behandling.typeBehandling() != TypeBehandling.Førstegangsbehandling) {
            // TODO: Sjekk på behandlingen og utled hva som har skjedd for å avgjøre om det skal sendes et brev
            return BrevBehov(null)
        }

        val underveisGrunnlag = underveisRepository.hent(behandlingId)

        val oppfyltePerioder = underveisGrunnlag.perioder.filter { it.utfall == Utfall.OPPFYLT }

        return if (oppfyltePerioder.isNotEmpty()) {
            // FIX LOGIKK
            // felles logikk her for når en behandling er innvilget
            // ved avslag: trenger prioritering på vilkår
            BrevBehov(TypeBrev.VEDTAK_INNVILGELSE)
        } else {
            BrevBehov(TypeBrev.VEDTAK_AVSLAG)
        }
    }
}
