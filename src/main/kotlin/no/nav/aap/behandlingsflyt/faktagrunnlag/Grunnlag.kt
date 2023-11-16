package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

interface Grunnlag {
    fun oppdater(kontekst: FlytKontekst): Boolean
}
