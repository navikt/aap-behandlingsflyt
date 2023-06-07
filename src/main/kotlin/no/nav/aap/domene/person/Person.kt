package no.nav.aap.domene.person

import no.nav.aap.domene.typer.Ident
import java.util.UUID

class Person(val identifikator: UUID, private var identer: List<Ident>) {

    fun er(ident: Ident): Boolean {
        return identer.any { it == ident }
    }

    fun identer(): List<Ident> {
        return identer.toList()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Person

        return identifikator == other.identifikator
    }

    override fun hashCode(): Int {
        return identifikator.hashCode()
    }

    override fun toString(): String {
        return "Person(identifikator=$identifikator, identer=$identer)"
    }
}
