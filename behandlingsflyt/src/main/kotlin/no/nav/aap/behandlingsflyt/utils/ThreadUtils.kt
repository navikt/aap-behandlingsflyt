package no.nav.aap.behandlingsflyt.utils

import org.slf4j.MDC
import java.util.function.Supplier


fun <U> withMdc(supplier: Supplier<U>): Supplier<U> {
    val mdc = MDC.getCopyOfContextMap()
    return Supplier {
        MDC.setContextMap(mdc)
        try {
            supplier.get()
        } finally {
            MDC.clear()
        }
    }
}