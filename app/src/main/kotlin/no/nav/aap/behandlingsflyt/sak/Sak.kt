package no.nav.aap.behandlingsflyt.sak

import no.nav.aap.behandlingsflyt.verdityper.Periode

class Sak(
    val id: SakId,
    val saksnummer: Saksnummer,
    val person: Person,
    val rettighetsperiode: Periode,
    private val status: Status = Status.OPPRETTET
) {

    fun status(): Status {
        return status
    }

    override fun toString(): String {
        return "Sak(id=$id, saksnummer='$saksnummer, person=$person, periode=$rettighetsperiode, status=$status')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Sak

        if (saksnummer != other.saksnummer) return false
        if (person != other.person) return false
        if (rettighetsperiode != other.rettighetsperiode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = saksnummer.hashCode()
        result = 31 * result + person.hashCode()
        result = 31 * result + rettighetsperiode.hashCode()
        return result
    }
}
