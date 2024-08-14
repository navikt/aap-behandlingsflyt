package no.nav.aap.verdityper.sakogbehandling

import kotlin.math.min

// TODO: vi antar at inlogget identifikator er aktiv, etter oppslag i PDL får vi den faktiske fasiten
class Ident(val identifikator: String, val aktivIdent: Boolean = true) {

    // TODO: skal equals/hashCode ta hensyn til aktivIdent?
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Ident

        return identifikator == other.identifikator
    }

    override fun hashCode(): Int {
        return identifikator.hashCode()
    }

    override fun toString(): String {
        return "Ident(identifikator='${identifikator.substring(0, min(identifikator.length, 6))}*****')"
    }


}
