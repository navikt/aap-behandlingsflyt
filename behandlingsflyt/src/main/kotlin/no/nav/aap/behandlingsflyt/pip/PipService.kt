package no.nav.aap.behandlingsflyt.pip

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.repository.RepositoryProvider

class PipService private constructor(private val repository: PipRepository) {

    constructor(repositoryProvider: RepositoryProvider) : this(repositoryProvider.provide())

    fun finnIdenterPåSak(saksnummer: Saksnummer): List<IdentPåSak> {
        if (!repository.sakEksisterer(saksnummer)) {
            return emptyList()
        }

        return repository.finnIdenterPåSak(saksnummer)
    }

    fun finnIdenterPåBehandling(referanse: BehandlingReferanse): List<IdentPåSak> {
        if (!repository.behandlingEksisterer(referanse)) {
            return emptyList()
        }

        return repository.finnIdenterPåBehandling(referanse)
    }
}
