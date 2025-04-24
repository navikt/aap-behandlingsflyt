package no.nav.aap.lookup.repository

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.aap.lookup.repository")

/**
 * Marker interface for repository.
 *
 * PS: Hver gang denne implementeres, må også App.kt oppdateres for at implementasjonene
 * skal lastes i [RepositoryRegistry].
 */
interface Repository {
    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        logger.warn("kopier-metoden er ikke implementert for ${this::class.simpleName}. Er dette korrekt? Hvis ikke, implementer dummy-metode.")
    }
}