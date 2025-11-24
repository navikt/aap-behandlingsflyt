package no.nav.aap.behandlingsflyt.pip

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.lookup.repository.Repository

interface PipRepository : Repository {
    fun sakEksisterer(saksnummer: Saksnummer): Boolean
    fun behandlingEksisterer(behandlingReferanse: BehandlingReferanse): Boolean
    fun finnIdenterPåSak(saksnummer: Saksnummer): List<IdentPåSak>
    fun finnIdenterPåBehandling(behandlingReferanse: BehandlingReferanse): List<IdentPåSak>
}