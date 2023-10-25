package no.nav.aap.behandlingsflyt.sak

import no.nav.aap.behandlingsflyt.sak.person.Person
import no.nav.aap.behandlingsflyt.domene.Periode
import java.util.*

class Sak(
    val id: Long,
    val person: Person,
    val rettighetsperiode: Periode,
    private var status: Status = Status.OPPRETTET
) {

    val saksnummer = Saksnummer(
        id.toString(36)
            .uppercase(Locale.getDefault())
            .replace("O", "o")
            .replace("I", "i")
    )

    fun status(): Status {
        return status
    }

    override fun toString(): String {
        return "Sak(id=$id, person=$person, periode=$rettighetsperiode, status=$status, saksnummer='$saksnummer')"
    }

}
