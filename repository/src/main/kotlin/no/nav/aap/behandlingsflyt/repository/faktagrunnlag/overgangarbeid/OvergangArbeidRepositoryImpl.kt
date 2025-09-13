package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.overgangarbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class OvergangArbeidRepositoryImpl(private val connection: DBConnection) : OvergangArbeidRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<OvergangArbeidRepositoryImpl> {
        override fun konstruer(connection: DBConnection): OvergangArbeidRepositoryImpl {
            return OvergangArbeidRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): OvergangArbeidGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT ID, VURDERINGER_ID
            FROM OVERGANG_ARBEID_GRUNNLAG
            WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                OvergangArbeidGrunnlag(
                    vurderinger = mapOvergangArbeidvurderinger(row.getLongOrNull("VURDERINGER_ID"))
                )
            }
        }
    }

    private fun mapOvergangArbeidvurderinger(overgangArbeidvurderingerId: Long?): List<OvergangArbeidVurdering> {
        return connection.queryList(
            """
                SELECT * FROM OVERGANG_ARBEID_VURDERING WHERE VURDERINGER_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, overgangArbeidvurderingerId)
            }
            setRowMapper(::overgangArbeidvurderingRowMapper)
        }
    }

    private fun overgangArbeidvurderingRowMapper(row: Row): OvergangArbeidVurdering {
        return OvergangArbeidVurdering(
            begrunnelse = row.getString("BEGRUNNELSE"),
            brukerRettPåAAP = row.getBoolean("BRUKER_RETT_PAA_AAP"),
            vurderingenGjelderFra = row.getLocalDate("VURDERINGEN_GJELDER_FRA"),
            vurderingenGjelderTil = row.getLocalDateOrNull("VURDERINGEN_GJELDER_TIL"),
            vurdertAv = row.getString("VURDERT_AV"),
            opprettet = row.getInstant("OPPRETTET_TID"),
            vurdertIBehandling = BehandlingId(row.getLong("VURDERT_I_BEHANDLING")),
        )
    }

    override fun lagre(behandlingId: BehandlingId, overgangArbeidVurderinger: List<OvergangArbeidVurdering>) {
        val overgangArbeidGrunnlag = hentHvisEksisterer(behandlingId)

        val nyttGrunnlag = OvergangArbeidGrunnlag(
            vurderinger = overgangArbeidVurderinger
        )

        if (overgangArbeidGrunnlag != nyttGrunnlag) {
            overgangArbeidGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val overgangArbeidVurderingerIds = getOvergangArbeidVurderingerIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from overgang_arbeid_grunnlag where behandling_id = ?; 
            delete from overgang_arbeid_vurdering where vurderinger_id = ANY(?::bigint[]);
            delete from overgang_arbeid_vurderinger where id = ANY(?::bigint[]);
           
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, overgangArbeidVurderingerIds)
                setLongArray(3, overgangArbeidVurderingerIds)
            }
        }
        log.info("Slettet $deletedRows rader fra overgang_arbeid_grunnlag")
    }

    private fun getOvergangArbeidVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT vurderinger_id
                    FROM overgang_arbeid_grunnlag
                    WHERE behandling_id = ? AND vurderinger_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurderinger_id")
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: OvergangArbeidGrunnlag) {
        val overgangArbeidvurderingerId = lagreOvergangArbeidvurderinger(nyttGrunnlag.vurderinger)

        connection.execute("INSERT INTO OVERGANG_ARBEID_GRUNNLAG (BEHANDLING_ID, VURDERINGER_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, overgangArbeidvurderingerId)
            }
        }
    }

    private fun lagreOvergangArbeidvurderinger(vurderinger: List<OvergangArbeidVurdering>): Long {
        val overgangarbeidvurderingerId = connection.executeReturnKey("""INSERT INTO OVERGANG_ARBEID_VURDERINGER DEFAULT VALUES""")

        connection.executeBatch(
            "INSERT INTO OVERGANG_ARBEID_VURDERING (BEGRUNNELSE, BRUKER_RETT_PAA_AAP, VURDERT_AV, VURDERINGER_ID, VURDERINGEN_GJELDER_FRA, VURDERINGEN_GJELDER_TIL, VURDERT_I_BEHANDLING, OPPRETTET_TID) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            vurderinger
        ) {
            setParams { vurdering ->
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.brukerRettPåAAP)
                setString(3, vurdering.vurdertAv)
                setLong(4, overgangarbeidvurderingerId)
                setLocalDate(5, vurdering.vurderingenGjelderFra)
                setLocalDate(6, vurdering.vurderingenGjelderTil)
                setLong(7, vurdering.vurdertIBehandling.id)
                setInstant(8, vurdering.opprettet)
            }
        }

        return overgangarbeidvurderingerId
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE OVERGANG_ARBEID_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
            INSERT INTO OVERGANG_ARBEID_GRUNNLAG (BEHANDLING_ID, VURDERINGER_ID) 
            SELECT ?, VURDERINGER_ID 
            FROM OVERGANG_ARBEID_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}