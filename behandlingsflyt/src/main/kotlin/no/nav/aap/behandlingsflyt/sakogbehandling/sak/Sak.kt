package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime

class Sak(
    val id: SakId,
    val saksnummer: Saksnummer,
    val person: Person,
    val rettighetsperiode: Periode,
    private val status: Status = Status.OPPRETTET,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
) {

    fun status(): Status {
        return status
    }

    fun rettighetsperiodeEttÅrFraStartDato(): Periode {
        return Periode(rettighetsperiode.fom, rettighetsperiode.fom.plusYears(1).minusDays(1))
    }

    override fun toString(): String {
        return "Sak(id=$id, saksnummer=$saksnummer, person=$person, rettighetsperiode=$rettighetsperiode, status=$status, opprettetTidspunkt=$opprettetTidspunkt)"
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
