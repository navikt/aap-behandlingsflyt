package no.nav.aap.behandlingsflyt.test

import no.nav.aap.komponenter.verdityper.Bruker

object TestBruker {
    val VEILDER = Bruker("V123456")
    val KVALITETSSIKRER = Bruker("K123456")
    val SAKSBEHANDLER = Bruker("S123456")
    val BESLUTTER = Bruker("B123456")
    val SUPERBRUKER = Bruker("X123456") // Bruker med tilgang til alt, men som ikke er en reell rolle i produksjon
}