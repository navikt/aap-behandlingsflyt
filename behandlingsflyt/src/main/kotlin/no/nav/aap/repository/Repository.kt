package no.nav.aap.repository

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

/**
 * Marker interface for repository.
 *
 * PS: Hver gang denne implementeres, må også App.kt oppdateres for at implementasjonene
 * skal lastes i [no.nav.aap.repository.RepositoryRegistry].
 */
interface Repository {
    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {}
}