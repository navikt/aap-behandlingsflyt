package no.nav.aap.behandlingsflyt.faktagrunnlag.legeerklæring

import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagstype

class Legeerklæring : Grunnlag {

    override fun oppdaterLegeerklæring(): Boolean {
        //TODO("Not yet implemented")
        return true
    }

    companion object : Grunnlagstype {
        override fun oppdater(grunnlag: List<Grunnlag>): Boolean {
            return grunnlag.all(Grunnlag::oppdaterLegeerklæring)
        }
    }
}
