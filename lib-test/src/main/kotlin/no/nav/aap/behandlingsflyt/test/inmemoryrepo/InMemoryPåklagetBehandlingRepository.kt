package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingMedReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryPåklagetBehandlingRepository : PåklagetBehandlingRepository {
    private val memory = HashMap<BehandlingId, PåklagetBehandlingGrunnlag>()
    override fun hentHvisEksisterer(behandlingId: BehandlingId): PåklagetBehandlingGrunnlag? {
        return memory[behandlingId]
    }

    override fun hentGjeldendeVurderingMedReferanse(behandlingReferanse: BehandlingReferanse): PåklagetBehandlingVurderingMedReferanse? {
        val behandling = InMemoryBehandlingRepository.hent(behandlingReferanse)

        return memory[behandling.id]?.vurdering?.let {
            PåklagetBehandlingVurderingMedReferanse(
                påklagetVedtakType = it.påklagetVedtakType,
                påklagetBehandling = it.påklagetBehandling,
                referanse = behandlingReferanse,
                vurdertAv = it.vurdertAv,
                opprettet = it.opprettet
            )
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        påklagetBehandlingVurdering: PåklagetBehandlingVurdering
    ) {
        TODO("Not yet implemented")
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }
}