package no.nav.aap.behandlingsflyt.sak

class Ident(val identifikator: String) {
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
        return "Ident(identifikator='${identifikator.substring(0, 6)}*****')"
    }


}
