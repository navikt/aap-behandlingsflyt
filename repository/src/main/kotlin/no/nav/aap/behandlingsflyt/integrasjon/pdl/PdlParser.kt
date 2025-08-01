package no.nav.aap.behandlingsflyt.integrasjon.pdl

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import java.time.Year

object PdlParser {
    fun utledFødselsdato(foedsels: List<PdlFoedsel>?): Fødselsdato? {
        val fødsel = foedsels
            ?.firstOrNull()

        if (fødsel == null) {
            return null
        }
        if (fødsel.foedselsdato != null) {
            return Fødselsdato.parse(fødsel.foedselsdato)
        }

        val foedselAar = fødsel.foedselAar
        return if (foedselAar != null) {
            Fødselsdato(Year.of(foedselAar.toInt()).atDay(1))
        } else {
            null
        }
    }
}