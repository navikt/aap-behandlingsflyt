package no.nav.aap.behandlingsflyt.mdc

import no.nav.aap.behandlingsflyt.log.ContextRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.MDC
import java.io.Closeable

class LoggingKontekst(repositoryProvider: RepositoryProvider, logKontekst: LogKontekst) :
    Closeable {

    private val keys = HashSet<String>()

    init {
        val contextRepository = repositoryProvider.provide(ContextRepository::class)
        if (logKontekst.referanse != null) {
            contextRepository.hentDataFor(logKontekst.referanse)?.forEach { key, value ->
                keys.add(key)
                MDC.put(key, value)
            }

        } else if (logKontekst.saksnummer != null) {
            contextRepository.hentDataFor(logKontekst.saksnummer)?.forEach { key, value ->
                keys.add(key)
                MDC.put(key, value)
            }
        }
    }

    override fun close() {
        keys.forEach { MDC.remove(it) }
    }
}