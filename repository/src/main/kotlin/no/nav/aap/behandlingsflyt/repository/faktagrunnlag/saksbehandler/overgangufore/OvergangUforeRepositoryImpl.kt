package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUforeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUforeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUforeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class OvergangUforeRepositoryImpl(private val connection: DBConnection) : OvergangUforeRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<OvergangUforeRepositoryImpl> {
        override fun konstruer(connection: DBConnection): OvergangUforeRepositoryImpl {
            return OvergangUforeRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): OvergangUforeGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT ID, VURDERINGER_ID
            FROM OVERGANG_UFORE_GRUNNLAG
            WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                OvergangUforeGrunnlag(
                    id = row.getLong("ID"),
                    vurderinger = mapOvergangUforevurderinger(row.getLongOrNull("BISTAND_VURDERINGER_ID"))
                )
            }
        }
    }

    private fun mapOvergangUforevurderinger(overgangUforevurderingerId: Long?): List<OvergangUforeVurdering> {
        return connection.queryList(
            """
                SELECT * FROM OVERGANG_UFORE_VURDERING WHERE OVERGANG_UFORE_VURDERINGER_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, overgangUforevurderingerId)
            }
            setRowMapper(::overgangUforevurderingRowMapper)
        }
    }

    private fun overgangUforevurderingRowMapper(row: Row): OvergangUforeVurdering {
        return OvergangUforeVurdering(
            begrunnelse = row.getString("BEGRUNNELSE"),
            brukerSoktUforetrygd = row.getBoolean("BRUKER_SOKT_UFORETRYGD"),
            brukerVedtakUforetrygd = row.getString("BRUKER_VEDTAK_UFORETRYGD"),
            brukerRettPaaAAP = row.getBooleanOrNull("BRUKER_RETT_PAA_AAP"),
            vurderingenGjelderFra = row.getLocalDateOrNull("VURDERINGEN_GJELDER_FRA"),
            virkningsDato = row.getLocalDateOrNull("VIRKNINGSDATO"),
            vurdertAv = row.getString("VURDERT_AV"),
            opprettet = row.getInstant("OPPRETTET_TID")
        )
    }

    override fun hentHistoriskeOvergangUforeVurderinger(sakId: SakId, behandlingId: BehandlingId): List<OvergangUforeVurdering> {
        val query = """
            SELECT DISTINCT overgang_ufore_vurdering.*
            FROM overgang_ufore_grunnlag grunnlag
            INNER JOIN overgang_ufore_vurderinger ON grunnlag.vurderinger_id = overgang_ufore_vurderinger.id
            INNER JOIN overgang_ufore_vurdering ON overgang_ufore_vurdering.vurderinger_id = overgang_ufore_vurderinger.id
            INNER JOIN behandling ON grunnlag.behandling_id = behandling.id
            WHERE grunnlag.aktiv AND behandling.sak_id = ?
                AND behandling.opprettet_tid < (select a.opprettet_tid from behandling a where id = ?)
            ORDER BY overgang_ufore_vurdering.opprettet_tid
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.id)
                setLong(2, behandlingId.id)
            }
            setRowMapper(::overgangUforevurderingRowMapper)
        }
    }

    override fun lagre(behandlingId: BehandlingId, overganguforevurderinger: List<OvergangUforeVurdering>) {
        val overgangUforeGrunnlag = hentHvisEksisterer(behandlingId)

        val nyttGrunnlag = OvergangUforeGrunnlag(
            id = null,
            vurderinger = overganguforevurderinger
        )

        if (overgangUforeGrunnlag != nyttGrunnlag) {
            overgangUforeGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val overgangUforeVurderingerIds = getOvergangUforeVurderingerIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from overgang_ufore_grunnlag where behandling_id = ?; 
            delete from overgang_ufore_vurdering where vurderinger_id = ANY(?::bigint[]);
            delete from overgang_ufore_vurderinger where id = ANY(?::bigint[]);
           
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, overgangUforeVurderingerIds)
                setLongArray(3, overgangUforeVurderingerIds)
            }
        }
        log.info("Slettet $deletedRows rader fra overgang_ufore_grunnlag")
    }

    private fun getOvergangUforeVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT vurderinger_id
                    FROM overgang_ufore_grunnlag
                    WHERE behandling_id = ? AND vurderinger_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurderinger_id")
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: OvergangUforeGrunnlag) {
        val overgangUforevurderingerId = lagreOvergangUforevurderinger(nyttGrunnlag.vurderinger)

        connection.execute("INSERT INTO OVERGANG_UFORE_GRUNNLAG (BEHANDLING_ID, VURDERINGER_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, overgangUforevurderingerId)
            }
        }
    }

    private fun lagreOvergangUforevurderinger(vurderinger: List<OvergangUforeVurdering>): Long {
        val overganguforevurderingerId = connection.executeReturnKey("""INSERT INTO OVERGANG_UFORE_VURDERINGER DEFAULT VALUES""")

        connection.executeBatch(
            "INSERT INTO OVERGANG_UFORE_GRUNNLAG (BEGRUNNELSE, BRUKER_SOKT_UFORETRYGD, BRUKER_VEDTAK_UFORETRYGD, BRUKER_RETT_PAA_AAP, VIRKNINGSDATO, VURDERT_AV, VURDERINGER_ID) VALUES (?, ?, ?, ?, ?, ?, ?)",
            vurderinger
        ) {
            setParams { vurdering ->
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.brukerSoktUforetrygd)
                setString(3, vurdering.brukerVedtakUforetrygd)
                setBoolean(4, vurdering.brukerRettPaaAAP)
                setLocalDate(5, vurdering.virkningsDato)
                setString(6, vurdering.vurdertAv)
                setLong(7, overganguforevurderingerId)
            }
        }

        return overganguforevurderingerId
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE OVERGANG_UFORE_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute(
            """
            INSERT INTO OVERGANG_UFORE_GRUNNLAG (BEHANDLING_ID, VURDERINGER_ID) 
            SELECT ?, VURDERINGER_ID 
            FROM OVERGANG_UFORE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}