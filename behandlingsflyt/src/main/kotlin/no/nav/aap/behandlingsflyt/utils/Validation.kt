package no.nav.aap.behandlingsflyt.utils


sealed class Validation<VALIDATEDCLASS>(
    val validatedObject: VALIDATEDCLASS
) {
    class Valid<VALIDATEDCLASS>(validatedObject: VALIDATEDCLASS) : Validation<VALIDATEDCLASS>(validatedObject)
    class Invalid<VALIDATEDCLASS>(validatedObject: VALIDATEDCLASS, val errorMessage: String) : Validation<VALIDATEDCLASS>(validatedObject)

    fun <NEW_TYPE>fold(onValid: (it: Valid<VALIDATEDCLASS>) -> NEW_TYPE, onInvalid: (it: Invalid<VALIDATEDCLASS>) -> NEW_TYPE): NEW_TYPE {
        return when(this) {
            is Valid -> onValid(this)
            is Invalid -> onInvalid(this)
        }
    }

    fun <NEW_TYPE>map(mapper: (it: VALIDATEDCLASS) -> NEW_TYPE): Validation<NEW_TYPE> {
        val newType = mapper(this.validatedObject)
        return when(this) {
            is Valid -> Valid(newType)
            is Invalid -> Invalid(newType, errorMessage)
        }
    }

    val isValid: Boolean get() = this is Valid
    val isInvalid: Boolean get() = this is Invalid

    fun onValid(block: (it: Valid<VALIDATEDCLASS>) -> Unit): Validation<VALIDATEDCLASS> {
        if(this is Valid) {
            block(this)
        }
        return this
    }

    fun onInvalid(block: (it: Invalid<VALIDATEDCLASS>) -> Unit): Validation<VALIDATEDCLASS> {
        if(this is Invalid) {
            block(this)
        }
        return this
    }

    fun throwOnInvalid(block: (it: Invalid<VALIDATEDCLASS>) -> Exception): Validation<VALIDATEDCLASS> {
        if (this is Invalid) {
            throw block(this)
        }
        return this
    }

    fun throwOnInvalid(): Validation<VALIDATEDCLASS> {
        return throwOnInvalid { RuntimeException(it.errorMessage) }
    }

    fun get(): VALIDATEDCLASS = when(this) {
        is Valid -> this.validatedObject
        is Invalid -> throw RuntimeException("Kan ikke hente et validert objekt når objektet ikke er validert som gyldig")
    }

    fun getOrNull(): VALIDATEDCLASS? = when(this) {
        is Valid -> this.validatedObject
        is Invalid -> null
    }

    fun getOrThrow(): VALIDATEDCLASS =
        getOrThrow { RuntimeException(it.errorMessage) }

    fun getOrThrow(block: (it: Invalid<VALIDATEDCLASS>) -> Exception): VALIDATEDCLASS = when(this) {
        is Valid -> this.validatedObject
        is Invalid -> throw block(this)
    }
}