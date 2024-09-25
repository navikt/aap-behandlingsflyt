package no.nav.aap.behandlingsflyt.test

import org.junit.jupiter.api.extension.ExtendWith

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.CLASS,
    AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(
    FakesExtension::class
)
annotation class Fakes