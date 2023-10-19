package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.faktagrunnlag.legeerklæring.Legeerklæringdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.Yrkesskadedata

interface Grunnlag {
    fun oppdaterYrkesskade(): Boolean {
        return true
    }

    fun hentYrkesskade(): Yrkesskadedata? {
        return null
    }

    fun oppdaterLegeerklæring(): Boolean {
        return true
    }

    fun hentLegeerklæring(): Legeerklæringdata? {
        return null
    }
}
