package no.nav.aap.behandlingsflyt.faktagrunnlag

interface Grunnlag {
    fun oppdaterYrkesskade(): Boolean {
        return true
    }

    fun oppdaterLegeerklæring(): Boolean {
        return true
    }
}
