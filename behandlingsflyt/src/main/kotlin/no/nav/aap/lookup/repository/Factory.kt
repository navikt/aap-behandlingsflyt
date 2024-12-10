package no.nav.aap.lookup.repository

import no.nav.aap.komponenter.dbconnect.DBConnection

/**
 * Factory interface for repository companion object
 */
interface Factory<T : Repository> {
    fun konstruer(connection: DBConnection): T
}