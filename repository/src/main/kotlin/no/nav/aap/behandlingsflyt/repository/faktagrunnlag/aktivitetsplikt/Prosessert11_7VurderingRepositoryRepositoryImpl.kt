package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Prosessert11_7VurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class Prosessert11_7VurderingRepositoryRepositoryImpl(private val connection: DBConnection) : Prosessert11_7VurderingRepository {
    companion object : Factory<Prosessert11_7VurderingRepositoryRepositoryImpl> {
        override fun konstruer(connection: DBConnection): Prosessert11_7VurderingRepositoryRepositoryImpl {
            return Prosessert11_7VurderingRepositoryRepositoryImpl(connection)
        }
    }
    
    override fun lagre(
        prosessertIBehandling: BehandlingId,
        aktivitetspliktBehandling: BehandlingId
    ) {
        val query = """
            INSERT INTO prosessert_11_7_vurdering (prosessert_i_behandling_id, aktivitetsplikt_behandling_id)
            VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, prosessertIBehandling.toLong())
                setLong(2, aktivitetspliktBehandling.toLong())
            }
        }
        
    }

    override fun nyesteProsesserteAktivitetspliktBehandling(prosessertIBehandling: BehandlingId): BehandlingId? {
        val query = """
            SELECT aktivitetsplikt_behandling_id
            FROM prosessert_11_7_vurdering 
            WHERE prosessert_i_behandling_id = ?
            ORDER BY opprettet_tid DESC
            LIMIT 1
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, prosessertIBehandling.toLong())
            }
            setRowMapper { row ->
                BehandlingId(row.getLong("aktivitetsplikt_behandling_id"))
            }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        // Ikke relevant
    }

    override fun slett(behandlingId: BehandlingId) {
        // Ikke relevant
    }
}