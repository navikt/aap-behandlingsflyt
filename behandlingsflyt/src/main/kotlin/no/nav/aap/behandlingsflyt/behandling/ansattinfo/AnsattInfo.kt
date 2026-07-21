package no.nav.aap.behandlingsflyt.behandling.ansattinfo

import no.nav.aap.komponenter.verdityper.Bruker

data class AnsattInfo(val navIdent: Bruker, val navn: String, val enhetsnummer: String)
data class AnsattVisningsnavn(val navident: Bruker, val visningsnavn: String)
