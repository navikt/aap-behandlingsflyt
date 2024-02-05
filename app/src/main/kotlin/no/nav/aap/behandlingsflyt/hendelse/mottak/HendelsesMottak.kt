package no.nav.aap.behandlingsflyt.hendelse.mottak

import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import javax.sql.DataSource

class HendelsesMottak(private val dataSource: DataSource) {

    fun håndtere(key: Ident, hendelse: PersonHendelse) {
        val saksnummer: Saksnummer? = dataSource.transaction { connection ->
            // todo: lage en asyncTransaction?
            runBlocking {
                PersonHendelsesHåndterer(connection).håndtere(key, hendelse)
            }
        }
        // Legg til kø for sak, men mocker ved å kalle videre bare
        if (saksnummer != null) {
            håndtere(saksnummer, hendelse.tilSakshendelse())
        }
    }

    fun håndtere(key: Saksnummer, hendelse: SakHendelse) {
        val sisteBehandling: BehandlingId? = dataSource.transaction { connection ->
            SakHendelsesHåndterer(connection).håndtere(key, hendelse)
        }
        if (sisteBehandling != null) {
            håndtere(sisteBehandling, hendelse.tilBehandlingHendelse())
        }
    }

    fun håndtere(key: BehandlingId, hendelse: BehandlingHendelse) {
        dataSource.transaction { connection ->
            BehandlingHendelseHåndterer(connection).håndtere(key, hendelse)
        }
    }
}
