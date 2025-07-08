package no.nav.aap.behandlingsflyt.sakogbehandling

import kotlin.math.min

// TODO: vi antar at inlogget identifikator er aktiv, etter oppslag i PDL får vi den faktiske fasiten
data class Ident(val identifikator: String, val aktivIdent: Boolean = true) {

    fun er(ident: Ident): Boolean {
        return identifikator == ident.identifikator
    }

    override fun toString(): String {
        return "Ident(identifikator='${identifikator.substring(0, min(identifikator.length, 6))}*****')"
    }
}
