package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.AndreYtelserSøknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.andreYtelserOppgittISøknad.AndreYtelserOppgittISøknadRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryAndreYtelserOppgittISøknadRepository : AndreYtelserOppgittISøknadRepository {
    private val opplysninger = ConcurrentHashMap<BehandlingId, AndreYtelserSøknad>()

    override fun lagre(behandlingId: BehandlingId, andreUtbetalinger: AndreYtelserSøknad) {
        opplysninger[behandlingId] = andreUtbetalinger.copy(stønad = requireNotNull(andreUtbetalinger.stønad).toList())
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): AndreYtelserSøknad? = opplysninger[behandlingId]

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        opplysninger[fraBehandling]?.let { opplysninger[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        opplysninger.remove(behandlingId)
    }
}
