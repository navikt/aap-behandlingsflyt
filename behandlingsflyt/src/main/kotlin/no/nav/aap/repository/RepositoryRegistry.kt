package no.nav.aap.repository

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

object RepositoryRegistry {
    private val registry = HashSet<KClass<Repository>>()
    fun register(repository: KClass<*>): RepositoryRegistry {
        validater(repository)

        registry.add(repository as KClass<Repository>)
        return this
    }

    private fun validater(klass: KClass<*>) {
        require(klass.starProjectedType.isSubtypeOf(Repository::class.starProjectedType)) {
            "Repository må være av variant Repository"
        }
        val companionObject = klass.companionObject
        requireNotNull(companionObject) {
            "Repository må ha companion object"
        }
        require(companionObject.isSubclassOf(Factory::class)) {
            "Repository må ha companion object av typen Factory"
        }
    }

    fun fetch(ktype: KType): KClass<Repository> {
        val singleOrNull = registry.singleOrNull { klass -> klass.starProjectedType.isSubtypeOf(ktype) }
        if (singleOrNull == null) {
            throw IllegalStateException("Repository av typen '$ktype' er ikke registrert")
        }
        return singleOrNull
    }
}