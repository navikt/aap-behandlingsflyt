package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BrevbestillingRepository(private val connection: DBConnection) {

    fun hent(referanse: BrevbestillingReferanse): Brevbestilling {
        val query =
            """
                SELECT *
                FROM BREVBESTILLING
                WHERE REFERANSE = ?
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setUUID(1, referanse.referanse)
            }
            setRowMapper { rowMapper(it) }
        }
    }

    fun hent(behandlingId: BehandlingId, typeBrev: TypeBrev): Brevbestilling? {
        val query =
            """
                SELECT *
                FROM BREVBESTILLING
                WHERE BEHANDLING_ID = ?
                AND TYPE_BREV = ?
            """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, typeBrev)
            }
            setRowMapper { rowMapper(it) }
        }
    }

    fun hent(behandlingId: BehandlingId): List<Brevbestilling> {
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

    private fun rowMapper(row: Row): Brevbestilling {
        return Brevbestilling(
            id = row.getLong("id"),
            behandlingId = BehandlingId(row.getLong("behandling_id")),
            typeBrev = row.getEnum("type_brev"),
            referanse = BrevbestillingReferanse(row.getUUID("referanse")),
            status = row.getEnum("status"),
        )
    }

    fun lagre(
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
                setUUID(3, bestillingReferanse.referanse)
                setEnumName(4, status)
            }
        }
    }

    fun oppdaterStatus(behandlingId: BehandlingId, referanse: BrevbestillingReferanse, status: Status) {
        val query =
            """
                UPDATE BREVBESTILLING SET STATUS = ? WHERE REFERANSE = ? AND BEHANDLING_ID = ?
            """.trimIndent()

        connection.execute(query) {
            setParams {
                setEnumName(1, status)
                setUUID(2, referanse.referanse)
                setLong(3, behandlingId.toLong())
            }
            setResultValidator {
                require(1 == it)
            }
        }
    }
}
