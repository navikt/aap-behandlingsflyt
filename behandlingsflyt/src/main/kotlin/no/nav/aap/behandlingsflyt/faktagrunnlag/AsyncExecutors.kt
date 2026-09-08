package no.nav.aap.behandlingsflyt.faktagrunnlag

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/*
 * Denne executoren lever så lenge JVM-prosessen lever (én per prosess i produksjon), og skal derfor
 * ikke lukkes av noen enkelt Application-instans sin nedstengingssekvens - det gir feil eierskap
 * og var årsaken til RejectedExecutionException i tester der flere Application-instanser deler samme JVM.
 * Lukking skjer i stedet her, samlokalisert med opprettelsen, via en JVM-shutdown-hook som gir
 * pågående oppgaver litt tid til å fullføre ved reell prosessavslutning.
 */
val informasjonskravExecutor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor().also { executor ->
    Runtime.getRuntime().addShutdownHook(
        Thread {
            executor.shutdown()
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        }
    )
}
