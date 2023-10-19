package no.nav.aap.behandlingsflyt.faktagrunnlag.legeerklæring

class Legeerklæringdata {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
