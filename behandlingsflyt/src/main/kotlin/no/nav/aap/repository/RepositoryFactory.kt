package no.nav.aap.repository

import no.nav.aap.komponenter.dbconnect.DBConnection
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.starProjectedType

class RepositoryFactory(val connection: DBConnection) {

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Repository> create(type: KClass<T>): T {
        val repositoryKlass = RepositoryRegistry.fetch(type.starProjectedType)
        return (repositoryKlass.companionObject as Factory<T>).konstruer(connection)
    }
}