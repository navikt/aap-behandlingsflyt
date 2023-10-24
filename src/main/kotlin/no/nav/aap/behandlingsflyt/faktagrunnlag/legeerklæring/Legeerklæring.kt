package no.nav.aap.behandlingsflyt.faktagrunnlag.legeerklæring

import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

internal class Legeerklæring : Grunnlag<Legeerklæringdata> {

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        //TODO("Not yet implemented")
        return true
    }

    override fun hent(kontekst: FlytKontekst): Legeerklæringdata? {
        TODO("Not yet implemented")
    }
}
