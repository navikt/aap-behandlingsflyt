package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import java.time.Instant

class InformasjonskravRepositoryImpl(
    private val connection: DBConnection,
): InformasjonkskravRepository {

    override fun hentOppdateringer(sakId: SakId, krav: List<InformasjonskravNavn>): List<InformasjonskravOppdatert> {
        return connection.queryList("""
            select distinct on (informasjonskrav) behandling_id, oppdatert, informasjonskrav
            from informasjonskrav_oppdatert
            where sak_id = ? and informasjonskrav = any(?::text[])
            order by informasjonskrav, oppdatert desc
        """) {
            setParams {
                setLong(1, sakId.toLong())
                setArray(2, krav.map { it.name })
            }
            setRowMapper {
                InformasjonskravOppdatert(
                    behandlingId = BehandlingId(it.getLong("behandling_id")),
                    navn = it.getEnum("informasjonskrav"),
                    oppdatert = it.getInstant("oppdatert"),
                )
            }
        }
    }

    override fun registrerOppdateringer(
        sakId: SakId,
        behandlingId: BehandlingId,
        informasjonskrav: List<InformasjonskravNavn>,
        oppdatert: Instant
    ) {
        connection.executeBatch("""
            insert into informasjonskrav_oppdatert (sak_id, behandling_id, informasjonskrav, oppdatert)
            values (?, ?, ?, ?)
        """.trimIndent(), informasjonskrav) {
            setParams { krav ->
                setLong(1, sakId.toLong())
                setLong(2, behandlingId.toLong())
                setEnumName(3, krav)
                setInstant(4, oppdatert)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        connection.execute("""
            delete from informasjonskrav_oppdatert where behandling_id = ? 
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Denne trengs ikke implementeres
    }

    companion object: Factory<InformasjonskravRepositoryImpl> {
        override fun konstruer(connection: DBConnection): InformasjonskravRepositoryImpl {
            return InformasjonskravRepositoryImpl(connection)
        }
    }
}