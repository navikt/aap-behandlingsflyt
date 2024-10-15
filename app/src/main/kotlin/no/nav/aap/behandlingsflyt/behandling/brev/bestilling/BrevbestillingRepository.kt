package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.util.UUID

class BrevbestillingRepository(private val connection: DBConnection) {

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
            setRowMapper {
                Brevbestilling(
                    behandlingId = BehandlingId(it.getLong("behandling_id")),
                    typeBrev = it.getEnum("type_brev"),
                    referanse = it.getUUID("referanse"),
                    status = it.getEnum("status"),
                )
            }
        }
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

    fun oppdaterStatus(behandlingId: BehandlingId, status: Status) {
        val query =
            """
                UPDATE BREVBESTILLING SET STATUS = ? WHERE BEHANDLING_ID = ?
            """.trimIndent()

        connection.execute(query) {
            setParams {
                setEnumName(1, status)
                setLong(1, behandlingId.toLong())
            }
            setResultValidator {
                require(1 == it)
            }
        }
    }
}
