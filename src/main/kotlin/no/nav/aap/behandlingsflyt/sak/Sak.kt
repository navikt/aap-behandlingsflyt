package no.nav.aap.behandlingsflyt.sak

import no.nav.aap.behandlingsflyt.Periode

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

}
