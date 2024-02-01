package no.nav.aap.behandlingsflyt.barnetillegg

import no.nav.aap.verdityper.sakogbehandling.Ident

class RettTilBarnetillegg {
    private val barn = mutableSetOf<Ident>()

    fun leggTilBarn(ident: Ident): RettTilBarnetillegg {
        barn.add(ident)
        return this
    }

    fun barn(): Set<Ident> {
        return barn.toSet()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RettTilBarnetillegg

        return barn == other.barn
    }

    override fun hashCode(): Int {
        return barn.hashCode()
    }

    override fun toString(): String {
        return "RettTilBarnetillegg(antallBarn=${barn.size})"
    }
}
