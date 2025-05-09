package no.nav.aap.lookup.repository

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

/**
 * Marker interface for repository.
 *
 * PS: Hver gang denne implementeres, må også App.kt oppdateres for at implementasjonene
 * skal lastes i [postgresRepositoryRegistry].
 */
interface Repository: no.nav.aap.komponenter.repository.Repository {
    /** Kopier opplysninger og vurderinger fra en behandling inn i en annen.
     *
     * Denne metoden kalles når en revurdering opprettes (`tilBehandling`). Ideen er at revurderingen
     * tar utgangspunkt i de vurderingene som allerede er gjort (`fraBehandling`). I revurderingen
     * kan man så legge til nye vurderinger uten at den gamle behandlingen blir påvirket.
     **/
    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}