package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.pdl.PdlFoedsel
import java.time.Year
import kotlin.text.toInt

object PdlParser {
    fun utledFødselsdato(foedsels: List<PdlFoedsel>?): Fødselsdato? {
        val fødsel = foedsels
            ?.firstOrNull()

        if (fødsel == null) {
            return null
        }
        if (fødsel.foedselsdato != null) {
            return Fødselsdato.parse(fødsel.foedselsdato!!)
        }

        val foedselAar = fødsel.foedselAar
        return if (foedselAar != null) {
            Fødselsdato(Year.of(foedselAar.toInt()).atDay(1))
        } else {
            null
        }
    }
}