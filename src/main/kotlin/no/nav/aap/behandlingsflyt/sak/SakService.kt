package no.nav.aap.behandlingsflyt.sak

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection

class SakService(transaksjonsconnection: DbConnection) {

    private val sakRepository = SakRepository

    fun hent(sakId: Long): Sak = sakRepository.hent(sakId)
    fun hent(saksnummer: Saksnummer): Sak = sakRepository.hent(saksnummer)
}