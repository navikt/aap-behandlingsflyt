package no.nav.aap.behandlingsflyt.faktagrunnlag

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object AsyncExecutors : AutoCloseable {
    private val informasjonskravExecutor = lazy {
        Executors.newVirtualThreadPerTaskExecutor()
    }

    val informasjonskrav: ExecutorService
        get() = informasjonskravExecutor.value

    override fun close() {
        if (informasjonskravExecutor.isInitialized()) {
            informasjonskravExecutor.value.close()
        }
    }
}
