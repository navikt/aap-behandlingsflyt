package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime

class Sak(
    val id: SakId,
    val saksnummer: Saksnummer,
    val person: Person,
    rettighetsperiode: Periode,
    private val status: Status = Status.OPPRETTET,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
) {

    /** Selv om rettighetsperiode settes når den endres i databasen, så er det i dag veldig mange kopier
     * av denne behandlingen som ikke blir mutert. Du burde derfor fortsatt lese fra databasen til vi
     * kommer dit at behandlingen injectes over alt i flyten. */
    var rettighetsperiode: Periode = rettighetsperiode
        internal set

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
