package no.nav.aap.behandlingsflyt.faktagrunnlag

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

val informasjonskravExecutor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
