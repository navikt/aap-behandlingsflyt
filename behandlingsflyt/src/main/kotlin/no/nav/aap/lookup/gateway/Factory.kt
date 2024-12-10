package no.nav.aap.lookup.gateway

/**
 * Factory interface for gateway companion object
 */
interface Factory<T : Gateway> {
    fun konstruer(): T
}