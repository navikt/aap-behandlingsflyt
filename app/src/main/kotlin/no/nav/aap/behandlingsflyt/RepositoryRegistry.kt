package no.nav.aap.behandlingsflyt

import no.nav.aap.repository.Factory
import no.nav.aap.repository.Repository
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

object RepositoryRegistry {
    private val registry = HashSet<KClass<Repository>>()
    fun register(repository: KClass<*>) {
        validater(repository)

        registry.add(repository as KClass<Repository>)
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
        return registry.single { klass -> klass.starProjectedType.isSubtypeOf(ktype) }
    }
}