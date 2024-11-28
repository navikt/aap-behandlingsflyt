package no.nav.aap.behandlingsflyt.test

import java.util.concurrent.atomic.AtomicLong

internal object Holder {
    private val løpenummer = AtomicLong(0)

    fun hent(): Long {
        return løpenummer.incrementAndGet()
    }
}