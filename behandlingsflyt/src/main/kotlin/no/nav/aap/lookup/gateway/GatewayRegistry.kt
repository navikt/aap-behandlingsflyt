package no.nav.aap.lookup.gateway

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

private val logger = LoggerFactory.getLogger(GatewayRegistry::class.java)

object GatewayRegistry {
    private val registry = HashSet<KClass<Gateway>>()
    private val lock = Object()

    inline fun <reified T : Gateway> register(): GatewayRegistry {
        return register(T::class)
    }

    fun <T : Gateway> register(repository: KClass<T>): GatewayRegistry {
        validater(repository)

        synchronized(lock) {
            // Kode for å støtte at tester kan legge inn varianter, burde potensielt vært skilt ut?
            val removedSomething = registry.removeIf { klass ->
                repository.supertypes.filter { type ->
                    type.isSubtypeOf(Gateway::class.starProjectedType)
                }.any { type -> klass.starProjectedType.isSubtypeOf(type) }
            }
            if (removedSomething) {
                logger.warn("Gateway '{}' var allerede registrert", repository)
            }
            @Suppress("UNCHECKED_CAST")
            registry.add(repository as KClass<Gateway>)
        }
        return this
    }

    private fun validater(klass: KClass<*>) {
        require(klass.starProjectedType.isSubtypeOf(Gateway::class.starProjectedType)) {
            "Gateway må være av variant Gateway"
        }
        val companionObject = klass.companionObject
        if (companionObject == null && klass.objectInstance != null) {
            return
        }
        requireNotNull(companionObject) {
            "Gateway må ha companion object"
        }
        require(companionObject.isSubclassOf(Factory::class)) {
            "Gateway må ha companion object av typen Factory"
        }
    }

    fun fetch(ktype: KType): KClass<Gateway> {
        synchronized(lock) {
            val singleOrNull = registry.singleOrNull { klass -> klass.starProjectedType.isSubtypeOf(ktype) }
            if (singleOrNull == null) {
                logger.warn("Gateway av typen '{}' er ikke registrert, har følgende '{}'", ktype, registry)
                throw IllegalStateException("Gateway av typen '$ktype' er ikke registrert")
            }
            return singleOrNull
        }
    }

    fun status() {
        logger.info(
            "{} gateway registrert har følgende '{}'",
            registry.size,
            registry.map { kclass -> kclass.starProjectedType })
    }
}