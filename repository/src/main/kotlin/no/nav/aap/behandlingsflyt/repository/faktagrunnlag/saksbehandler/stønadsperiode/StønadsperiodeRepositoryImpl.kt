package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.stønadsperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.Instant

class StønadsperiodeRepositoryImpl(private val connection: DBConnection) : StønadsperiodeRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<StønadsperiodeRepository> {
        override fun konstruer(connection: DBConnection): StønadsperiodeRepository {
            return StønadsperiodeRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, vurderinger: List<StønadsperiodeVurdering>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = StønadsperiodeGrunnlag(vurderinger)

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            deaktiverEksisterendeGrunnlag(behandlingId)
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: StønadsperiodeGrunnlag) {
        val vurderingerId = lagreStønadsperiodeVurderinger(nyttGrunnlag.vurderinger)
        connection.execute(
            """
                INSERT INTO stonadsperiode_grunnlag (behandling_id, stonadsperiode_vurderinger_id, opprettet_tid) VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingerId)
                setInstant(3, Instant.now())
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): StønadsperiodeGrunnlag? {
        val query = """
            SELECT * FROM stonadsperiode_grunnlag WHERE behandling_id = ? AND aktiv = TRUE
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapGrunnlag)
        }
    }

    private fun mapGrunnlag(row: Row): StønadsperiodeGrunnlag {
        val vurderingerId = row.getLongOrNull("stonadsperiode_vurderinger_id")

        return StønadsperiodeGrunnlag(
            connection.queryList(
                """
            SELECT
                v.referanse                         AS v_referanse,
                v.begrunnelse                       AS v_begrunnelse,
                v.har_hatt_ordinar_siste_52_uker    AS v_har_hatt_ordinar_siste_52_uker,
                v.har_gjenvaerende_kvote            AS v_har_gjenvaerende_kvote,
                v.relevant_krav_type                AS v_relevant_krav_type,
                v.vurdert_i_behandling              AS v_vurdert_i_behandling,
                v.vurdert_tidspunkt                 AS v_vurdert_tidspunkt,
                v.vurdert_av                        AS v_vurdert_av
            FROM stonadsperiode_vurdering v 
            WHERE v.stonadsperiode_vurderinger_id = ?
        """.trimIndent()
            ) {
                setParams {
                    setLong(1, vurderingerId)
                }
                setRowMapper {
                    StønadsperiodeVurdering(
                        referanse = Kravreferanse(it.getUUID("v_referanse")),
                        begrunnelse = it.getString("v_begrunnelse"),
                        harHattOrdinærSiste52Uker = it.getBoolean("v_har_hatt_ordinar_siste_52_uker"),
                        harGjenværendeKvote = it.getBoolean("v_har_gjenvaerende_kvote"),
                        relevantKravType = it.getEnum("v_relevant_krav_type"),
                        vurdertIBehandling = BehandlingId(it.getLong("v_vurdert_i_behandling")),
                        opprettet = it.getInstant("v_vurdert_tidspunkt"),
                        vurdertAv = it.getBruker("v_vurdert_av"),
                    )
                }
            }
        )
    }

    override fun tilbakestillGrunnlag(behandlingId: BehandlingId, forrigeBehandling: BehandlingId?) {
        deaktiverEksisterendeGrunnlag(behandlingId)

        if (forrigeBehandling != null) {
            kopier(forrigeBehandling, behandlingId)
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        hentHvisEksisterer(fraBehandling) ?: return

        connection.execute(
            """
                INSERT INTO stonadsperiode_grunnlag (behandling_id, stonadsperiode_vurderinger_id, opprettet_tid)
                SELECT ?, stonadsperiode_vurderinger_id, ?
                FROM stonadsperiode_grunnlag
                WHERE aktiv AND behandling_id = ?
                """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setInstant(2, Instant.now())
                setLong(3, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val vurderingIds = getStønadsperiodeVurderingerIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            DELETE FROM stonadsperiode_grunnlag WHERE behandling_id = ?;
            DELETE FROM stonadsperiode_vurdering WHERE stonadsperiode_vurderinger_id = ANY(?::bigint[]);
            DELETE FROM stonadsperiode_vurderinger WHERE id = ANY(?::bigint[]);
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, vurderingIds)
                setLongArray(3, vurderingIds)
            }
        }

        log.info("Slettet $deletedRows rader for stonadsperiode på behandling ${behandlingId.id}")
    }

    private fun hentGrunnlagsId(behandlingsId: BehandlingId): Long? {
        return connection.queryFirstOrNull<Long>(
            "SELECT id FROM stonadsperiode_grunnlag WHERE behandling_id = ? AND aktiv = TRUE"
        ) {
            setParams {
                setLong(1, behandlingsId.toLong())
            }
            setRowMapper { it.getLong("id") }
        }
    }

    private fun deaktiverEksisterendeGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE stonadsperiode_grunnlag SET aktiv = FALSE WHERE aktiv AND behandling_id = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }

    private fun getStønadsperiodeVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
        SELECT stonadsperiode_vurderinger_id
        FROM stonadsperiode_grunnlag
        WHERE behandling_id = ? AND stonadsperiode_vurderinger_id IS NOT NULL
        """.trimIndent()
    ) {
        setParams {
            setLong(1, behandlingId.id)
        }
        setRowMapper {
            it.getLong("stonadsperiode_vurderinger_id")
        }
    }

    private fun lagreStønadsperiodeVurderinger(vurderinger: List<StønadsperiodeVurdering>): Long {
        val vurderingerId = connection.executeReturnKey(
            "INSERT INTO stonadsperiode_vurderinger (opprettet_tid) VALUES (?)"
        ) {
            setParams {
                setInstant(1, Instant.now())
            }
        }

        val query = """
                INSERT INTO stonadsperiode_vurdering 
                (referanse, begrunnelse, har_hatt_ordinar_siste_52_uker, har_gjenvaerende_kvote, relevant_krav_type, vurdert_i_behandling, vurdert_tidspunkt, stonadsperiode_vurderinger_id, vurdert_av) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        connection.executeBatch(query, vurderinger) {
            setParams { vurdering ->
                setUUID(1, vurdering.referanse.verdi)
                setString(2, vurdering.begrunnelse)
                setBoolean(3, vurdering.harHattOrdinærSiste52Uker)
                setBoolean(4, vurdering.harGjenværendeKvote)
                setEnumName(5, vurdering.relevantKravType)
                setLong(6, vurdering.vurdertIBehandling.toLong())
                setInstant(7, vurdering.opprettet)
                setLong(8, vurderingerId)
                setBruker(9, vurdering.vurdertAv)
            }
        }

        return vurderingerId
    }
}
