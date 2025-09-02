package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.overganguføre

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
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
                    id = row.getLong("ID"),
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
            brukerRettPaaAAP = row.getBooleanOrNull("BRUKER_RETT_PAA_AAP"),
            vurderingenGjelderFra = row.getLocalDateOrNull("VURDERINGEN_GJELDER_FRA"),
            virkningsDato = row.getLocalDateOrNull("VIRKNINGSDATO"),
            vurdertAv = row.getString("VURDERT_AV"),
            opprettet = row.getInstant("OPPRETTET_TID")
        )
    }

    override fun hentHistoriskeOvergangArbeidVurderinger(sakId: SakId, behandlingId: BehandlingId): List<OvergangArbeidVurdering> {
        val query = """
            SELECT DISTINCT overgang_arbeid_vurdering.*
            FROM overgang_arbeid_grunnlag grunnlag
            INNER JOIN overgang_arbeid_vurderinger ON grunnlag.vurderinger_id = overgang_arbeid_vurderinger.id
            INNER JOIN overgang_arbeid_vurdering ON overgang_arbeid_vurdering.vurderinger_id = overgang_arbeid_vurderinger.id
            INNER JOIN behandling ON grunnlag.behandling_id = behandling.id
            WHERE grunnlag.aktiv AND behandling.sak_id = ?
                AND behandling.opprettet_tid < (select a.opprettet_tid from behandling a where id = ?)
            ORDER BY overgang_arbeid_vurdering.opprettet_tid
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.id)
                setLong(2, behandlingId.id)
            }
            setRowMapper(::overgangArbeidvurderingRowMapper)
        }
    }

    override fun lagre(behandlingId: BehandlingId, overgangarbeidvurderinger: List<OvergangArbeidVurdering>) {
        val overgangArbeidGrunnlag = hentHvisEksisterer(behandlingId)

        val nyttGrunnlag = OvergangArbeidGrunnlag(
            id = null,
            vurderinger = overgangarbeidvurderinger
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
            "INSERT INTO OVERGANG_ARBEID_VURDERING (BEGRUNNELSE, BRUKER_RETT_PAA_AAP, VIRKNINGSDATO, VURDERT_AV, VURDERINGER_ID, VURDERINGEN_GJELDER_FRA) VALUES (?, ?, ?, ?, ?, ?)",
            vurderinger
        ) {
            setParams { vurdering ->
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.brukerRettPaaAAP)
                setLocalDate(3, vurdering.virkningsDato)
                setString(4, vurdering.vurdertAv)
                setLong(5, overgangarbeidvurderingerId)
                setLocalDate(6, vurdering.vurderingenGjelderFra)
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