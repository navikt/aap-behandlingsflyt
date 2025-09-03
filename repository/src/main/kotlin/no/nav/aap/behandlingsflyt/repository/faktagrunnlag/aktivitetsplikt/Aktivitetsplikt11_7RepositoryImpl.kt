package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class Aktivitetsplikt11_7RepositoryImpl(private val connection: DBConnection) : Aktivitetsplikt11_7Repository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<Aktivitetsplikt11_7RepositoryImpl> {
        override fun konstruer(connection: DBConnection): Aktivitetsplikt11_7RepositoryImpl {
            return Aktivitetsplikt11_7RepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Aktivitetsplikt11_7Grunnlag? {
        val query = """
            select * from aktivitetsplikt_11_7_vurdering v
            left join aktivitetsplikt_11_7_grunnlag g on v.id = g.vurdering_id
            where g.aktiv = true and g.behandling_id = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapGrunnlag)
        }
    }

    override fun hentHistoriskeVurderinger(
        sakId: SakId,
        behandlingId: BehandlingId
    ): List<Aktivitetsplikt11_7Vurdering> {
        val query = """
            select v.* 
            from aktivitetsplikt_11_7_vurdering v
            inner join aktivitetsplikt_11_7_grunnlag g on v.id = g.vurdering_id
            inner join behandling b on g.behandling_id = b.id
            where g.aktiv 
            and b.sak_id = ?
            and b.opprettet_tid < (select a.opprettet_tid from behandling a where id = ?)
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
                setLong(2, behandlingId.toLong())
            }
            setRowMapper(::mapVurdering)
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: Aktivitetsplikt11_7Vurdering
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = Aktivitetsplikt11_7Grunnlag(vurdering = vurdering)

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
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            return
        }
        lagre(tilBehandling, eksisterendeGrunnlag)
    }

    override fun slett(behandlingId: BehandlingId) {
        // TODO: Avgjør om vi trenger dette. Gjør ingenting inntil videre
        log.warn("Forsøkte å slette aktivitetsplikt-grunnlag, men sletting er ikke implementert")
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: Aktivitetsplikt11_7Grunnlag) {
        val vurderingId = lagreVurdering(nyttGrunnlag.vurdering)
        val query = """
            insert into aktivitetsplikt_11_7_grunnlag 
            (behandling_id, vurdering_id, aktiv) 
            values (?, ?, true)
        """.trimIndent()
        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    private fun lagreVurdering(vurdering: Aktivitetsplikt11_7Vurdering): Long {
        val query = """
            INSERT INTO aktivitetsplikt_11_7_vurdering 
            (begrunnelse, er_oppfylt, utfall, vurdert_av, vurderingen_gjelder_fra, opprettet_tid) 
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erOppfylt)
                setEnumName(3, vurdering.utfall)
                setString(4, vurdering.vurdertAv)
                setLocalDate(5, vurdering.gjelderFra)
                setInstant(6, vurdering.opprettet)
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE AKTIVITETSPLIKT_11_7_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    private fun mapGrunnlag(row: Row): Aktivitetsplikt11_7Grunnlag {
        return Aktivitetsplikt11_7Grunnlag(
            vurdering = mapVurdering(row)
        )
    }

    private fun mapVurdering(row: Row): Aktivitetsplikt11_7Vurdering {
        return Aktivitetsplikt11_7Vurdering(
            begrunnelse = row.getString("begrunnelse"),
            erOppfylt = row.getBoolean("er_oppfylt"),
            utfall = row.getStringOrNull("utfall")?.let { Utfall.valueOf(it) },
            vurdertAv = row.getString("vurdert_av"),
            gjelderFra = row.getLocalDate("vurderingen_gjelder_fra"),
            opprettet = row.getInstant("opprettet_tid")
        )
    }
}