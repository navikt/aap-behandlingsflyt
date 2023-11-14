package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.sak.Ident

object PersonRegisterMock {

    private var personer = HashMap<Ident, Personopplysning>()

    private val LOCK = Object()

    fun innhent(identer: List<Ident>): Set<Personopplysning> {
        synchronized(LOCK) {
            return personer
                .filterKeys { ident -> ident in identer }
                .map { it.value }
                .toSet()
        }
    }

    fun konstruer(ident: Ident, personopplysning: Personopplysning) {
        synchronized(LOCK) {
            personer[ident] = personopplysning
        }
    }
}
