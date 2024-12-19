package no.nav.aap.lookup.repository

import no.nav.aap.komponenter.dbconnect.DBConnection
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType

class RepositoryProvider(private val connection: DBConnection) {

    inline fun <reified T : Repository> provide(type: KClass<T>): T {
        val repositoryKlass = RepositoryRegistry.fetch(type.starProjectedType)

        return internalCreate(repositoryKlass)
    }

    inline fun <reified T : Repository> internalCreate(repositoryKlass: KClass<Repository>): T {
        val companionObjectType = repositoryKlass.companionObject
        if (companionObjectType == null && repositoryKlass.objectInstance != null
            && repositoryKlass.isSubclassOf(Repository::class)
        ) {
            return repositoryKlass.objectInstance as T
        }

        val companionObject = repositoryKlass.companionObjectInstance
        requireNotNull(companionObject) {
            "Repository må ha companion object"
        }
        if (companionObject is Factory<*>) {
            return companionObject.konstruer(connection()) as T
        }
        throw IllegalStateException("Repository må ha et companion object som implementerer Factory<T> interfacet.")
    }

    fun provideAlle(): List<Repository> {
        return RepositoryRegistry.alle().map { klass -> internalCreate(klass) }
    }

    /**
     * NB! Ikke til bruk utenfor denne klassen
     *
     * Er kun synlig for inline reified functions ovenfor
     */
    fun connection(): DBConnection {
        return connection
    }
}