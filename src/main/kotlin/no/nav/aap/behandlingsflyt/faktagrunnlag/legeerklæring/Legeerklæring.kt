package no.nav.aap.behandlingsflyt.faktagrunnlag.legeerklæring

import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagstype

internal class Legeerklæring : Grunnlag {

    override fun oppdaterLegeerklæring(): Boolean {
        //TODO("Not yet implemented")
        return true
    }

    internal companion object : Grunnlagstype<Legeerklæringdata>() {
        override fun oppdater(grunnlag: List<Grunnlag>): Boolean {
            return grunnlag.all(Grunnlag::oppdaterLegeerklæring)
        }

        override fun hentGrunnlag(grunnlag: List<Grunnlag>): Legeerklæringdata? {
            return grunnlag.firstNotNullOfOrNull(Grunnlag::hentLegeerklæring)
        }
    }
}
