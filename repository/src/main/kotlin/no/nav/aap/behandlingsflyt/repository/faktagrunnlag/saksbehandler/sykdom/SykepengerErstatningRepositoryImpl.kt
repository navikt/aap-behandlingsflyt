package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory

class SykepengerErstatningRepositoryImpl(private val connection: DBConnection) :
    SykepengerErstatningRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<SykepengerErstatningRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SykepengerErstatningRepositoryImpl {
            return SykepengerErstatningRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, vurderinger: List<SykepengerVurdering>) {
        deaktiverGrunnlag(behandlingId)
        val grunnlagId = opprettTomtGrunnlag(behandlingId)
        lagreVurderingerPåGrunnlag(grunnlagId, vurderinger)
    }

    private fun opprettTomtGrunnlag(behandlingId: BehandlingId): Long {
        val vurderingerId = connection.executeReturnKey("INSERT INTO SYKEPENGE_VURDERINGER DEFAULT VALUES;")

        return connection.executeReturnKey("INSERT INTO SYKEPENGE_ERSTATNING_GRUNNLAG (behandling_id, vurderinger_id) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingerId)
            }
        }
    }

    private fun lagreVurderingerPåGrunnlag(grunnlagId: Long, vurderinger: List<SykepengerVurdering>) {
        if (vurderinger.isNotEmpty()) {
            val vurderingerId =
                connection.queryFirst<Long>("SELECT vurderinger_id FROM SYKEPENGE_ERSTATNING_GRUNNLAG where id=?") {
                    setParams { setLong(1, grunnlagId) }
                    setRowMapper { it.getLong("vurderinger_id") }
                }

            val insertQuery = """
                INSERT INTO SYKEPENGE_VURDERING (begrunnelse, oppfylt, grunn, vurdert_av, gjelder_fra, gjelder_tom, vurderinger_id, vurdert_i_behandling, opprettet_tid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            vurderinger.forEach { vurdering ->
                connection.executeReturnKey(insertQuery) {
                    setParams {
                        setString(1, vurdering.begrunnelse)
                        setBoolean(2, vurdering.harRettPå)
                        setEnumName(3, vurdering.grunn)
                        setString(4, vurdering.vurdertAv)
                        setLocalDate(5, vurdering.gjelderFra)
                        setLocalDate(6, vurdering.gjelderTom)
                        setLong(7, vurderingerId)
                        setLong(8, vurdering.vurdertIBehandling.toLong())
                        setLocalDateTime(9, vurdering.vurdertTidspunkt ?: java.time.LocalDateTime.now())
                    }
                }

            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SYKEPENGE_ERSTATNING_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        hentHvisEksisterer(fraBehandling) ?: return

        val query = """
            INSERT INTO SYKEPENGE_ERSTATNING_GRUNNLAG (behandling_id, vurderinger_id)
            SELECT ?, vurderinger_id from SYKEPENGE_ERSTATNING_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SykepengerErstatningGrunnlag? {
        val query = """
            SELECT * FROM SYKEPENGE_ERSTATNING_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                SykepengerErstatningGrunnlag(
                    row.getLongOrNull("vurderinger_id")?.let(::mapVurdering).orEmpty()
                )
            }
        }
    }

    private fun mapVurdering(vurderingerId: Long): List<SykepengerVurdering> {
        val query = """
            SELECT * FROM SYKEPENGE_VURDERING WHERE vurderinger_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper { row ->
                SykepengerVurdering(
                    begrunnelse = row.getString("begrunnelse"),
                    harRettPå = row.getBoolean("oppfylt"),
                    grunn = row.getEnumOrNull("grunn"),
                    vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
                    vurdertAv = row.getString("vurdert_av"),
                    vurdertTidspunkt = row.getLocalDateTime("opprettet_tid"),
                    gjelderFra = row.getLocalDate("gjelder_fra"),
                    gjelderTom = row.getLocalDateOrNull("gjelder_tom"),
                )
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val sykepengeVurderingerIds = getSykepengeVurderingerIds(behandlingId)
        val deletedRows = connection.executeReturnUpdated(
            """
            delete from sykepenge_erstatning_grunnlag where behandling_id = ?; 
            delete from sykepenge_vurdering where vurderinger_id = ANY(?::bigint[]);
            delete from sykepenge_vurderinger where id = ANY(?::bigint[]);
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, sykepengeVurderingerIds)
                setLongArray(3, sykepengeVurderingerIds)
            }
        }
        log.info("Slettet $deletedRows rader fra sykepenge_erstatning_grunnlag")
    }

    private fun getSykepengeVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT vurderinger_id
                    FROM sykepenge_erstatning_grunnlag
                    WHERE behandling_id = ? AND vurderinger_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurderinger_id")
        }
    }
}
