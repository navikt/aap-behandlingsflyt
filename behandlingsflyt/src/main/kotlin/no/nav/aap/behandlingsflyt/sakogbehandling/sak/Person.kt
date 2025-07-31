package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import java.util.*

@JvmInline
value class PersonId(val id: Long)

class Person(val id: PersonId, val identifikator: UUID, private var identer: List<Ident>) {

    constructor(identifikator: UUID, identer: List<Ident>) : this(PersonId(-0), identifikator, identer)

    fun er(ident: Ident): Boolean {
        return identer.any { it == ident }
    }

    fun aktivIdent(): Ident {
        return identer().first { it.aktivIdent }
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
