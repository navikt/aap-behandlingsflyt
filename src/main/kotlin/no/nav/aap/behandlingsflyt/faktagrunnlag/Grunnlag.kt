package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

interface Grunnlag<GRUNNLAGSDATA> {
    fun oppdater(kontekst: FlytKontekst): Boolean
    fun hent(kontekst: FlytKontekst): GRUNNLAGSDATA?
}
