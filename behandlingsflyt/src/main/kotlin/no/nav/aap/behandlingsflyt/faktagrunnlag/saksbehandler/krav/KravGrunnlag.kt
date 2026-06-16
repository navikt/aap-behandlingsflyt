package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

data class KravGrunnlag(
    val vurderinger: Set<KravVurdering>,
) {
    fun gjeldendeVedtatteVurderinger(behandlingId: BehandlingId): Set<KravVurdering> {
        return vurderinger
            .filter { it.vurdertIBehandling != behandlingId }
            .groupBy { it.journalpostId } // TODO: Bytt denne ut med referanse
            .values
            .map { kravForReferanse -> kravForReferanse.maxBy { it.opprettet } }
            .toSet()
    }
}

