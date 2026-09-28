package no.nav.aap.behandlingsflyt.faktagrunnlag

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/*
 * Denne executoren lever så lenge JVM-prosessen lever (én per prosess i produksjon), og skal derfor
 * ikke lukkes av noen enkelt Application-instans sin nedstengingssekvens. Trådene som opprettes er
 * virtuelle tråder, som alltid er daemon-tråder - de hindrer derfor ikke JVM-en i å avslutte, og
 * krever ingen eksplisitt shutdown. En egen JVM-shutdown-hook her ville uansett kunne race med
 * Motor sin (asynkrone, lengre) nedstengingssekvens og forårsake RejectedExecutionException for
 * jobber som fortsatt kjører.
 */
val informasjonskravExecutor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
