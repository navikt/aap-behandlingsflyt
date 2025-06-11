package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtakForPerson
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person

class PåklagetBehandlingVurderingService(
    val behandlingRepository: BehandlingRepository,
    val påklagetBehandlingRepository: PåklagetBehandlingRepository
) {
    fun hentGjeldendeVurderingMedReferanse(behandlingsreferanse: BehandlingReferanse): PåklagetBehandlingVurderingMedReferanse? {
        return påklagetBehandlingRepository.hentGjeldendeVurderingMedReferanse(behandlingsreferanse)
    }

    @Deprecated("Bruk hentAlleBehandlingerMedVedtakForPerson istedet")
    fun hentAlleBehandlingerMedVedtakForSak(behandlingsreferanse: BehandlingReferanse): List<BehandlingMedVedtak> {
        val behandling = behandlingRepository.hent(behandlingsreferanse)
        return behandlingRepository.hentAlleMedVedtakFor(
            behandling.sakId,
            listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
        )
    }

    fun hentAlleBehandlingerMedVedtakForPerson(person: Person): List<BehandlingMedVedtakForPerson> {
        return behandlingRepository.hentAlleMedVedtakFor(person, listOf(TypeBehandling.Revurdering))
    }

}
