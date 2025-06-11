package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person

class PåklagetBehandlingVurderingService(
    val behandlingRepository: BehandlingRepository,
    val påklagetBehandlingRepository: PåklagetBehandlingRepository
) {
    fun hentGjeldendeVurderingMedReferanse(behandlingsreferanse: BehandlingReferanse): PåklagetBehandlingVurderingMedReferanse? {
        return påklagetBehandlingRepository.hentGjeldendeVurderingMedReferanse(behandlingsreferanse)
    }

    fun hentAlleBehandlingerMedVedtakForPerson(person: Person): List<BehandlingMedVedtak> {
        return behandlingRepository.hentAlleMedVedtakFor(person, TypeBehandling.entries.filter { it != TypeBehandling.Klage })
    }

}
