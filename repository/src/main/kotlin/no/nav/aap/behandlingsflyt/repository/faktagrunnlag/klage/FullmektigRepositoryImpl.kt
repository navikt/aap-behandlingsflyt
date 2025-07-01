package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.IdentMedType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.NavnOgAdresse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class FullmektigRepositoryImpl(private val connection: DBConnection) : FullmektigRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object : Factory<FullmektigRepositoryImpl> {
        override fun konstruer(connection: DBConnection): FullmektigRepositoryImpl {
            return FullmektigRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): FullmektigGrunnlag? {
        val query = """
            SELECT * FROM FULLMEKTIG_VURDERING
            WHERE id IN (
                SELECT vurdering_id FROM FULLMEKTIG_GRUNNLAG
                WHERE BEHANDLING_ID = ? AND AKTIV = TRUE
            )
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapGrunnlag)
        }
    }

    override fun lagre(behandlingId: BehandlingId, vurdering: FullmektigVurdering) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = FullmektigGrunnlag(vurdering = vurdering)
        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: FullmektigGrunnlag) {
        val vurderingId = lagreVurdering(nyttGrunnlag.vurdering)
        val query = """
            INSERT INTO FULLMEKTIG_GRUNNLAG (BEHANDLING_ID, VURDERING_ID, AKTIV) 
            VALUES (?, ?, TRUE)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    private fun lagreVurdering(vurdering: FullmektigVurdering): Long {
        val query = """
            INSERT INTO FULLMEKTIG_VURDERING 
            (har_fullmektig, fullmektig_ident, fullmektig_ident_type, fullmektig_navn_og_adresse, vurdert_av)
            VALUES (?, ?, ?, ?::jsonb, ?)
        """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setBoolean(1, vurdering.harFullmektig)
                setString(2, vurdering.fullmektigIdent?.ident)
                setEnumName(3, vurdering.fullmektigIdent?.type)
                setString(4, vurdering.fullmektigNavnOgAdresse?.let { DefaultJsonMapper.toJson(it) })
                setString(5, vurdering.vurdertAv)
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // TODO: Avklar om vi trenger flere behandlinger per klage
        // Gjør ingenting
    }

    override fun slett(behandlingId: BehandlingId) {
        val vurderingIds = getIdForVurderingerForGrunnlaget(behandlingId)

        val deletedRowsGrunnlag = connection.executeReturnUpdated(
            """
            delete from fullmektig_grunnlag where behandling_id = ?; 
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
        }

        val deletedRowsVurdering = connection.executeReturnUpdated(
            """
            delete from fullmektig_vurdering where id = ANY(?::bigint[]);
        """.trimIndent()
        ) {
            setParams {
                setLongArray(1, vurderingIds)
            }
        }

        log.info("Slettet $deletedRowsGrunnlag rader fra fullmektig_grunnlag og $deletedRowsVurdering rader fra fullmektig_vurdering")
    }

    private fun getIdForVurderingerForGrunnlaget(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                SELECT vurdering_id
                FROM fullmektig_grunnlag
                WHERE behandling_id = ? AND vurdering_id is not null
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurdering_id")
        }
    }

    private fun mapGrunnlag(row: Row): FullmektigGrunnlag {
        return FullmektigGrunnlag(
            vurdering = mapFullmektigVurdering(row),
        )
    }

    private fun mapFullmektigVurdering(row: Row): FullmektigVurdering {
        return FullmektigVurdering(
            harFullmektig = row.getBoolean("har_fullmektig"),
            fullmektigNavnOgAdresse = row.getStringOrNull("fullmektig_navn_og_adresse")
                ?.let { DefaultJsonMapper.fromJson<NavnOgAdresse>(it) },
            fullmektigIdent = row.getStringOrNull("fullmektig_ident")
                ?.let { IdentMedType(it, row.getEnum("fullmektig_ident_type")) },
            vurdertAv = row.getString("VURDERT_AV"),
            opprettet = row.getInstant("opprettet_tid"),
        )
    }


    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE FULLMEKTIG_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }
}