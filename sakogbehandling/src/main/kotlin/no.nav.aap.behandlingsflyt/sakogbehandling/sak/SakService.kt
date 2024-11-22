package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.SakId

class SakService(private val sakRepository: SakRepository) {

    fun hent(sakId: SakId): Sak {
        return sakRepository.hent(sakId)
    }

    fun hent(saksnummer: Saksnummer): Sak {
        return sakRepository.hent(saksnummer)
    }
}
