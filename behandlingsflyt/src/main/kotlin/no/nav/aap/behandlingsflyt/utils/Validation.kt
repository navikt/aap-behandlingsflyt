package no.nav.aap.behandlingsflyt.utils

sealed class Validation<VALIDATEDCLASS>(
    val validatedObject: VALIDATEDCLASS
) {
    class Valid<VALIDATEDCLASS>(validatedObject: VALIDATEDCLASS) : Validation<VALIDATEDCLASS>(validatedObject)
    class Invalid<VALIDATEDCLASS>(validatedObject: VALIDATEDCLASS, val errorMessage: String) : Validation<VALIDATEDCLASS>(validatedObject)
}