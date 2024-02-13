package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class Personopplysning(
    val fødselsdato: Fødselsdato,
    val opprettetTid: LocalDateTime = LocalDateTime.now(),
) {

    // Denne skal kun sammenlikne data og ikke tidspunkter
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Personopplysning

        return fødselsdato == other.fødselsdato
    }

    override fun hashCode(): Int {
        return fødselsdato.hashCode()
    }

    fun skalInnhentes(): Boolean {
        val iGår = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(1)
        return opprettetTid.truncatedTo(ChronoUnit.DAYS).isBefore(iGår)
    }
}
