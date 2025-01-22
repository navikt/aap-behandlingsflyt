package no.nav.aap.behandlingsflyt.log

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.lookup.repository.Repository

interface ContextRepository : Repository {

    fun hentDataFor(behandlingReferanse: BehandlingReferanse): Map<String, String>?

    fun hentDataFor(saksnummer: Saksnummer): Map<String, String>?
}