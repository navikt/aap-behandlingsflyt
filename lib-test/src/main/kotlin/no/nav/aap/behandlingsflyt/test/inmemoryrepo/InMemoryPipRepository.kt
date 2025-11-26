package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.pip.IdentPåSak
import no.nav.aap.behandlingsflyt.pip.PipRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryPipRepository : PipRepository {
    override fun sakEksisterer(saksnummer: Saksnummer): Boolean {
        return true
    }

    override fun behandlingEksisterer(behandlingReferanse: BehandlingReferanse): Boolean {
        return true
    }

    override fun finnIdenterPåSak(saksnummer: Saksnummer): List<IdentPåSak> {
        return listOf(
            IdentPåSak(
                ident = InMemorySakRepository.hent(saksnummer).person.aktivIdent().identifikator,
                opprinnelse = IdentPåSak.Opprinnelse.PERSON
            )
        )
    }

    override fun finnIdenterPåBehandling(behandlingReferanse: BehandlingReferanse): List<IdentPåSak> {
        val sak = InMemorySakRepository.hent(InMemoryBehandlingRepository.hent(behandlingReferanse).sakId)
        return listOf(
            IdentPåSak(
                ident = sak.person.aktivIdent().identifikator,
                opprinnelse = IdentPåSak.Opprinnelse.PERSON
            )
        )
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
    }

    override fun slett(behandlingId: BehandlingId) {
    }
}