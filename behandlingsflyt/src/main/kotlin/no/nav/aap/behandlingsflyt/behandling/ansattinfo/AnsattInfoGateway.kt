package no.nav.aap.behandlingsflyt.behandling.ansattinfo

import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.verdityper.Bruker


interface AnsattInfoGateway : Gateway {
    fun hentAnsattInfo(navIdent: Bruker): AnsattInfo
    fun hentAnsatteVisningsnavn(navIdenter: List<Bruker>): List<AnsattVisningsnavn?>
}
