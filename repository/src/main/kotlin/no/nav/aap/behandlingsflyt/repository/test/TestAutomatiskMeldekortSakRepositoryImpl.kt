package no.nav.aap.behandlingsflyt.repository.test

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.TestAutomatiskMeldekortSakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class TestAutomatiskMeldekortSakRepositoryImpl(private val connection: DBConnection) :
    TestAutomatiskMeldekortSakRepository {

    companion object : Factory<TestAutomatiskMeldekortSakRepositoryImpl> {
        override fun konstruer(connection: DBConnection): TestAutomatiskMeldekortSakRepositoryImpl =
            TestAutomatiskMeldekortSakRepositoryImpl(connection)
    }

    override fun leggTil(sakId: SakId) {
        connection.execute(
            "INSERT INTO test_automatisk_meldekort_sak (sak_id) VALUES (?) ON CONFLICT DO NOTHING"
        ) {
            setParams {
                setLong(1, sakId.toLong())
            }
        }
    }

    override fun eksisterer(sakId: SakId): Boolean {
        return connection.queryFirst("SELECT EXISTS(SELECT 1 FROM test_automatisk_meldekort_sak WHERE sak_id = ?)") {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper {
                it.getBoolean("exists")
            }
        }
    }

    override fun hentAlle(): List<SakId> =
        connection.queryList("SELECT sak_id FROM test_automatisk_meldekort_sak") {
            setRowMapper { row -> SakId(row.getLong("sak_id")) }
        }

    // Tabellen er sak-basert og har ingen behandlingsdata å kopiere eller slette
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) = Unit

    override fun slett(behandlingId: BehandlingId) = Unit
}
