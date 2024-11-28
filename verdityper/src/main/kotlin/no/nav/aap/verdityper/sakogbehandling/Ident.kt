package no.nav.aap.verdityper.sakogbehandling

import kotlin.math.min

// TODO: vi antar at inlogget identifikator er aktiv, etter oppslag i PDL får vi den faktiske fasiten
class Ident(val identifikator: String, val aktivIdent: Boolean = true) {

    fun er(ident: Ident): Boolean {
        return identifikator == ident.identifikator
    }

    override fun toString(): String {
        return "Ident(identifikator='${identifikator.substring(0, min(identifikator.length, 6))}*****')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Ident

        if (identifikator != other.identifikator) return false
        if (aktivIdent != other.aktivIdent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = identifikator.hashCode()
        result = 31 * result + aktivIdent.hashCode()
        return result
    }
}
