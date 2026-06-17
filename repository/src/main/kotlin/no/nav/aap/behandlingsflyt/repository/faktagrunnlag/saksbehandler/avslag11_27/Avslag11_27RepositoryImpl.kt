package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.avslag11_27

import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Grunnlag
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Repository
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Vurdering
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory
import java.time.Instant

class Avslag11_27RepositoryImpl(private val connection: DBConnection) : Avslag11_27Repository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<Avslag11_27Repository> {
        override fun konstruer(connection: DBConnection): Avslag11_27Repository {
            return Avslag11_27RepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, vurderinger: List<Avslag11_27Vurdering>) {
        if (hentHvisEksisterer(behandlingId) != null) {
            deaktiverEksisterendeGrunnlag(behandlingId)
        }

        val vurderingerId = lagreAvslag11_27Vurderinger(vurderinger)
        connection.execute(
            """
                INSERT INTO avslag_11_27_grunnlag (behandling_id, avslag_11_27_vurderinger_id, opprettet_tid) VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingerId)
                setInstant(3, Instant.now())
            }
        }

    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Avslag11_27Grunnlag? {
        val vurderinger = connection.queryList(
            """
            SELECT
                v.journalpost_id                  AS v_journalpost_id,
                v.begrunnelse                     AS v_begrunnelse,
                v.har_annen_full_ytelse           AS v_har_annen_full_ytelse,
                v.brukers_ytelse                  AS v_brukers_ytelse,
                v.har_sykepengegrunnlag_over_2g   AS v_har_sykepengegrunnlag_over_2g,
                v.skal_avslaas_1127               AS v_skal_avslaas_1127,
                v.vurdert_i_behandling            AS v_vurdert_i_behandling,
                v.vurdert_tidspunkt               AS v_vurdert_tidspunkt,
                v.vurdert_av                      AS v_vurdert_av
            FROM avslag_11_27_grunnlag g
            LEFT JOIN avslag_11_27_vurdering v ON v.avslag_11_27_vurderinger_id = g.avslag_11_27_vurderinger_id
            WHERE g.aktiv = true AND g.behandling_id = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper {
                Avslag11_27Vurdering(
                    journalpostId = JournalpostId(it.getString("v_journalpost_id")),
                    begrunnelse = it.getString("v_begrunnelse"),
                    harAnnenFullYtelse = it.getBoolean("v_har_annen_full_ytelse"),
                    brukersYtelse = it.getStringOrNull("v_brukers_ytelse")?.let { Ytelse.valueOf(it) },
                    harSykepengegrunnlagOver2G = it.getBooleanOrNull("v_har_sykepengegrunnlag_over_2g"),
                    skalAvslås1127 = it.getBoolean("v_skal_avslaas_1127"),
                    vurdertIBehandling = BehandlingId(it.getLong("v_vurdert_i_behandling")),
                    vurdertTidspunkt = it.getInstant("v_vurdert_tidspunkt"),
                    vurdertAv = Bruker(it.getString("v_vurdert_av"))
                )
            }
        }

        if (vurderinger.isEmpty()) return null

        return Avslag11_27Grunnlag(
            vurderinger = vurderinger
        )
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)

        connection.execute(
            """INSERT INTO avslag_11_27_grunnlag (behandling_id, avslag_11_27_vurderinger_id)
                SELECT ?, avslag_11_27_vurderinger_id
                FROM avslag_11_27_grunnlag
                WHERE aktiv AND behandling_id = ?""".trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val vurderingIds = getAvslag11_27VurderingerIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            DELETE FROM avslag_11_27_grunnlag WHERE behandling_id = ?;
            DELETE FROM avslag_11_27_vurdering WHERE avslag_11_27_vurderinger_id = ANY(?::bigint[]);
            DELETE FROM avslag_11_27_vurderinger WHERE id = ANY(?::bigint[]);
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, vurderingIds)
            }
        }

        log.info("Slettet $deletedRows rader for avslag_11_27")
    }

    private fun deaktiverEksisterendeGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE avslag_11_27_grunnlag SET aktiv = FALSE WHERE aktiv AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    private fun getAvslag11_27VurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
        SELECT avslag_11_27_vurderinger_id
        FROM avslag_11_27_grunnlag
        WHERE behandling_id = ? AND avslag_11_27_vurderinger_id is not null
        """.trimIndent()
    ) {
        setParams {
            setLong(1, behandlingId.id)
        }
        setRowMapper {
            it.getLong("avslag_11_27_vurderinger_id")
        }
    }

    private fun lagreAvslag11_27Vurderinger(vurderinger: List<Avslag11_27Vurdering>): Long? {
        if (vurderinger.isEmpty()) return null

        val vuderingerId = connection.executeReturnKey(
            "INSERT INTO avslag_11_27_vurderinger (opprettet_tid) VALUES (?)"
        ) {
            setParams {
                setInstant(1, Instant.now())
            }
        }

        val query = """
                INSERT INTO avslag_11_27_vurdering 
                (journalpost_id, begrunnelse, har_annen_full_ytelse, brukers_ytelse, har_sykepengegrunnlag_over_2g, skal_avslaas_1127, vurdert_i_behandling, vurdert_tidspunkt, vurdert_av, avslag_11_27_vurderinger_id) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?,?)
            """.trimIndent()

        connection.executeBatch(query, vurderinger) {
            setParams { vurdering ->
                setString(1, vurdering.journalpostId.toString())
                setString(2, vurdering.begrunnelse)
                setBoolean(3, vurdering.harAnnenFullYtelse)
                setString(4, vurdering.brukersYtelse?.name)
                setBoolean(5, vurdering.harSykepengegrunnlagOver2G)
                setBoolean(6, vurdering.skalAvslås1127)
                setLong(7, vurdering.vurdertIBehandling.toLong())
                setInstant(8, vurdering.vurdertTidspunkt)
                setString(9, vurdering.vurdertAv.toString())
                setLong(10, vuderingerId)
            }
        }

        return vuderingerId
    }

}