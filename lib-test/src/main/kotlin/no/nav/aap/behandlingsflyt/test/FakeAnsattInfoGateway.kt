package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfo
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoGateway
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattVisningsnavn
import no.nav.aap.komponenter.gateway.Factory

class FakeAnsattInfoGateway: AnsattInfoGateway {
    override fun hentAnsattInfo(navIdent: String): AnsattInfo {
        return AnsattInfo(
            navIdent = navIdent,
            navn = "Ansatt Saksbehandler",
            enhetsnummer = "0300"
        )
    }

    override fun hentAnsatteVisningsnavn(navIdenter: List<String>): List<AnsattVisningsnavn?> {
        return navIdenter.map { AnsattVisningsnavn(
            navident = it,
            visningsnavn = it
        ) }
    }

    companion object : Factory<AnsattInfoGateway> {
        override fun konstruer(): AnsattInfoGateway = FakeAnsattInfoGateway()
    }

}