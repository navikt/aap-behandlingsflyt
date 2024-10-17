package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.brev.Status
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.util.UUID

class BrevbestillingRepository(private val connection: DBConnection) {

    fun hent(referanse: UUID): Brevbestilling {
        val query =
            """
                SELECT *
                FROM BREVBESTILLING
                WHERE REFERANSE = ?
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setUUID(1, referanse)
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

    private fun rowMapper(row: Row): Brevbestilling {
        return Brevbestilling(
            id = row.getLong("id"),
            behandlingId = BehandlingId(row.getLong("behandling_id")),
            typeBrev = row.getEnum("type_brev"),
            referanse = row.getUUID("referanse"),
            status = row.getEnum("status"),
        )
    }

    fun lagre(
        behandlingId: BehandlingId,
        typeBrev: TypeBrev,
        bestillingReferanse: UUID,
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
                setUUID(3, bestillingReferanse)
                setEnumName(4, status)
            }
        }
    }

    fun oppdaterStatus(referanse: UUID, status: Status) {
        val query =
            """
                UPDATE BREVBESTILLING SET STATUS = ? WHERE REFERANSE = ?
            """.trimIndent()

        connection.execute(query) {
            setParams {
                setEnumName(1, status)
                setUUID(1, referanse)
            }
            setResultValidator {
                require(1 == it)
            }
        }
    }
}
