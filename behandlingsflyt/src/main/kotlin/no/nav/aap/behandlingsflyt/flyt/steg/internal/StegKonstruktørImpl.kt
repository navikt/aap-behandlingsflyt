package no.nav.aap.behandlingsflyt.flyt.steg.internal

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegKonstruktør
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.lookup.repository.RepositoryRegistry

class StegKonstruktørImpl(private val repositoryProvider: RepositoryProvider) : StegKonstruktør {
    override fun konstruer(steg: FlytSteg): BehandlingSteg {
        return steg.konstruer(repositoryProvider)
    }
}