package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Vurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class Aktivitetsplikt11_9RepositoryImpl(private val connection: DBConnection) : Aktivitetsplikt11_9Repository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<Aktivitetsplikt11_9RepositoryImpl> {
        override fun konstruer(connection: DBConnection): Aktivitetsplikt11_9RepositoryImpl {
            return Aktivitetsplikt11_9RepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Aktivitetsplikt11_9Grunnlag? {
        val query = """
            select v.* 
            from aktivitetsplikt_11_9_grunnlag g
            inner join aktivitetsplikt_11_9_vurderinger vs on g.vurderinger_id = vs.id
            inner join aktivitetsplikt_11_9_vurdering v  on vs.id = v.vurderinger_id
            where g.aktiv = true and g.behandling_id = ?
        """.trimIndent()

        val vurderinger = connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapVurdering)
        }
        return if (vurderinger.isEmpty()) {
            null
        } else {
            Aktivitetsplikt11_9Grunnlag(vurderinger = vurderinger)
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vurderinger: List<Aktivitetsplikt11_9Vurdering>
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = Aktivitetsplikt11_9Grunnlag(vurderinger = vurderinger)

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(tilBehandling)
        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(tilBehandling)
        }

        val query = """
            insert into aktivitetsplikt_11_9_grunnlag (behandling_id, vurderinger_id, aktiv)
            select ?, vurderinger_id, true
            from aktivitetsplikt_11_9_grunnlag
            where behandling_id = ? and aktiv
        """.trimIndent()
        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        // TODO: Avgjør om vi trenger dette. Gjør ingenting inntil videre
        log.warn("Forsøkte å slette aktivitetsplikt-grunnlag, men sletting er ikke implementert")
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: Aktivitetsplikt11_9Grunnlag) {
        val vurderingerId = lagreVurderinger(nyttGrunnlag.vurderinger)
        val query = """
            insert into aktivitetsplikt_11_9_grunnlag 
            (behandling_id, vurderinger_id, aktiv) 
            values (?, ?, true)
        """.trimIndent()
        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingerId)
            }
        }
    }

    private fun lagreVurderinger(vurderinger: List<Aktivitetsplikt11_9Vurdering>): Long {
        val vurderingerId = connection.executeReturnKey(
            """
            insert into aktivitetsplikt_11_9_vurderinger default values
        """.trimIndent()
        )

        val query = """
            INSERT INTO aktivitetsplikt_11_9_vurdering 
            (begrunnelse, grunn, brudd, vurdert_av, dato, opprettet_tid, vurderinger_id, vurdert_i_behandling) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(query, vurderinger) {
            setParams { vurdering ->
                setString(1, vurdering.begrunnelse)
                setEnumName(2, vurdering.grunn)
                setEnumName(3, vurdering.brudd)
                setString(4, vurdering.vurdertAv)
                setLocalDate(5, vurdering.dato)
                setInstant(6, vurdering.opprettet)
                setLong(7, vurderingerId)
                setLong(8, vurdering.vurdertIBehandling.toLong())
            }
        }

        return vurderingerId
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE AKTIVITETSPLIKT_11_9_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    private fun mapVurdering(row: Row): Aktivitetsplikt11_9Vurdering {
        return Aktivitetsplikt11_9Vurdering(
            begrunnelse = row.getString("begrunnelse"),
            dato = row.getLocalDate("dato"),
            grunn = row.getEnum("grunn"),
            brudd = row.getEnum("brudd"),
            vurdertAv = row.getString("vurdert_av"),
            opprettet = row.getInstant("opprettet_tid"),
            vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
        )
    }
}