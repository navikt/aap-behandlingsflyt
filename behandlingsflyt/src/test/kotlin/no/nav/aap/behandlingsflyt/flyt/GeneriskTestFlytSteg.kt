package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.lookup.repository.RepositoryProvider

class GeneriskTestFlytSteg(private val stegType: StegType) : FlytSteg {
    override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
        return GeneriskTestSteg()
    }

    override fun type(): StegType {
        return stegType
    }
}
