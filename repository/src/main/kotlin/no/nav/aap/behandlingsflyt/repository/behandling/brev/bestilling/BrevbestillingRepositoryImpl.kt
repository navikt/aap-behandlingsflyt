package no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.*
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory

class BrevbestillingRepositoryImpl(private val connection: DBConnection) :
    BrevbestillingRepository {

    companion object : Factory<BrevbestillingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): BrevbestillingRepositoryImpl {
            return BrevbestillingRepositoryImpl(connection)
        }
    }

    override fun hent(behandlingId: BehandlingId): List<Brevbestilling> {
        val query =
            """
                SELECT *
                FROM BREVBESTILLING
                WHERE BEHANDLING_ID = ?
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { rowMapper(it) }
        }
    }

    override fun hent(brevbestillingReferanse: BrevbestillingReferanse): Brevbestilling {
        val query =
            """
                SELECT *
                FROM BREVBESTILLING
                WHERE REFERANSE = ?
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setUUID(1, brevbestillingReferanse.brevbestillingReferanse)
            }
            setRowMapper { rowMapper(it) }
        }
    }

    private fun rowMapper(row: Row): Brevbestilling {
        return Brevbestilling(
            id = row.getLong("id"),
            behandlingId = BehandlingId(row.getLong("behandling_id")),
            typeBrev = row.getEnum("type_brev"),
            referanse = BrevbestillingReferanse(row.getUUID("referanse")),
            status = row.getEnum("status"),
            opprettet = row.getLocalDateTime("opprettet"),
        )
    }

    override fun lagre(
        behandlingId: BehandlingId,
        typeBrev: TypeBrev,
        bestillingReferanse: BrevbestillingReferanse,
        status: Status
    ) {
        val query =
            """
                INSERT INTO BREVBESTILLING (BEHANDLING_ID, TYPE_BREV, REFERANSE, STATUS)
                VALUES (?, ?, ?, ?)
            """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, typeBrev)
                setUUID(3, bestillingReferanse.brevbestillingReferanse)
                setEnumName(4, status)
            }
        }
    }

    override fun oppdaterStatus(behandlingId: BehandlingId, referanse: BrevbestillingReferanse, status: Status) {
        val query =
            """
                UPDATE BREVBESTILLING SET STATUS = ? WHERE REFERANSE = ? AND BEHANDLING_ID = ?
            """.trimIndent()

        connection.execute(query) {
            setParams {
                setEnumName(1, status)
                setUUID(2, referanse.brevbestillingReferanse)
                setLong(3, behandlingId.toLong())
            }
            setResultValidator {
                require(1 == it)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        // Det trenger ikke å slettes fra brevBestilling-tabellen, men det må slettes fra aap-brev - applikasjonen.
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Trengs ikke implementeres
    }
}