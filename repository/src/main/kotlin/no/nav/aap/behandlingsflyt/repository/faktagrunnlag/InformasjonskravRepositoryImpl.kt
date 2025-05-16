package no.nav.aap.behandlingsflyt.repository.faktagrunnlag

import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonkskravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.Instant

class InformasjonskravRepositoryImpl(
    private val connection: DBConnection,
): InformasjonkskravRepository {

    private val log = LoggerFactory.getLogger(javaClass)

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
        val deletedRows = connection.executeReturnUpdated("""
            delete from informasjonskrav_oppdatert where behandling_id = ? 
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
            }
        }
        log.info("Slettet $deletedRows fra mottatt_dokument")
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