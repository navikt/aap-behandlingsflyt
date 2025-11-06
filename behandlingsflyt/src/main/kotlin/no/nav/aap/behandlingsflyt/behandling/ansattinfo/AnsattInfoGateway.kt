package no.nav.aap.behandlingsflyt.behandling.ansattinfo

import no.nav.aap.komponenter.gateway.Gateway


interface AnsattInfoGateway : Gateway {
    fun hentAnsattInfo(navIdent: String): AnsattInfo
    fun hentAnsatteVisningsnavn(navIdenter: List<String>): List<AnsattVisningsnavn?>
}
