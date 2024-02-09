package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGateway
import no.nav.aap.verdityper.sakogbehandling.Ident

object FakePersonopplysningGateway : PersonopplysningGateway {

    private var personer = HashMap<Ident, Personopplysning>()

    private val LOCK = Object()

    override suspend fun innhent(identer: List<Ident>): Personopplysning? {
        synchronized(LOCK) {
            return personer
                .filterKeys { ident -> ident in identer }
                .map { it.value }
                .first()
        }
    }

    fun konstruer(ident: Ident, personopplysning: Personopplysning) {
        synchronized(LOCK) {
            personer[ident] = personopplysning
        }
    }
}
