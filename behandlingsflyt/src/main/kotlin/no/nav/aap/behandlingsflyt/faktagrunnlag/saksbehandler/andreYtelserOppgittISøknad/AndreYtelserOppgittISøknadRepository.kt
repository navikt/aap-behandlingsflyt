package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.andreYtelserOppgittISøknad

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalinger
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface AndreYtelserOppgittISøknadRepository : Repository {
    fun lagre(behandlingId: BehandlingId, andreUtbetalinger: AndreUtbetalinger)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): AndreUtbetalinger?
    fun hent(behandlingId: BehandlingId): AndreUtbetalinger
}