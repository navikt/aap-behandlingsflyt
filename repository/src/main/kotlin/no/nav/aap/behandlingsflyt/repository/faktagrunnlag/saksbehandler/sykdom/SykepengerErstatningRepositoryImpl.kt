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
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        val nyttGrunnlag = SykepengerErstatningGrunnlag(vurderinger = vurderinger)

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            lagreGrunnlag(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagreGrunnlag(behandlingId: BehandlingId, nyttGrunnlag: SykepengerErstatningGrunnlag) {
        val vurderinger = nyttGrunnlag.vurderinger
        var vurderingerId: Long? = null
        if (vurderinger.isNotEmpty()) {
            val insert = """
                INSERT INTO SYKEPENGE_VURDERINGER DEFAULT VALUES;
            """.trimIndent()
            vurderingerId = connection.executeReturnKey(insert)

            val query = """
            INSERT INTO SYKEPENGE_VURDERING (begrunnelse, oppfylt, grunn, vurdert_av, gjelder_fra, vurderinger_id)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()

            vurderinger.forEach { vurdering ->
                val vurderingId = connection.executeReturnKey(query) {
                    setParams {
                        setString(1, vurdering.begrunnelse)
                        setBoolean(2, vurdering.harRettPå)
                        setEnumName(3, vurdering.grunn)
                        setString(4, vurdering.vurdertAv)
                        setLocalDate(5, vurdering.gjelderFra)
                        setLong(6, vurderingerId)
                    }
                }

                // TODO: flytt denne inn som array i vurdering-tabellen, slik at vi kan
                // gjøre executeBatch
                lagreDokument(vurderingId, vurdering.dokumenterBruktIVurdering)
            }
        }
        val grunnlagQuery = """
            INSERT INTO SYKEPENGE_ERSTATNING_GRUNNLAG (behandling_id, vurderinger_id) VALUES (?, ?)
        """.trimIndent()

        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingerId)
            }
        }
    }

    private fun lagreDokument(vurderingId: Long, journalpostIdEr: List<JournalpostId>) {
        val query = """
            INSERT INTO SYKEPENGE_VURDERING_DOKUMENTER (vurdering_id, journalpost) 
            VALUES (?, ?)
        """.trimIndent()

        connection.executeBatch(query, journalpostIdEr) {
            setParams { journalpostId ->
                setLong(1, vurderingId)
                setString(2, journalpostId.identifikator)
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SYKEPENGE_ERSTATNING_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
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
                    dokumenterBruktIVurdering = hentDokumenter(row.getLong("id")),
                    harRettPå = row.getBoolean("oppfylt"),
                    grunn = row.getEnumOrNull("grunn"),
                    vurdertAv = row.getString("vurdert_av"),
                    vurdertTidspunkt = row.getLocalDateTime("opprettet_tid"),
                    gjelderFra = row.getLocalDateOrNull("gjelder_fra")
                )
            }
        }
    }

    private fun hentDokumenter(vurderingId: Long): List<JournalpostId> {
        val query = """
            SELECT journalpost FROM SYKEPENGE_VURDERING_DOKUMENTER WHERE vurdering_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper { row ->
                JournalpostId(row.getString("journalpost"))
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val sykepengeVurderingIds = getSykepengeVurderingIds(behandlingId)
        val deletedRows = connection.executeReturnUpdated(
            """
            delete from sykepenge_erstatning_grunnlag where behandling_id = ?; 
            delete from sykepenge_vurdering_dokumenter where vurdering_id = ANY(?::bigint[]);
            delete from sykepenge_vurdering where id = ANY(?::bigint[]);
            delete from sykepenge_vurderinger where id in (select id from sykepenge_erstatning_grunnlag where behandling_id = ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, sykepengeVurderingIds)
                setLongArray(3, sykepengeVurderingIds)
                setLong(4, behandlingId.id)
            }
        }
        log.info("Slettet $deletedRows rader fra sykepenge_erstatning_grunnlag")
    }

    private fun getSykepengeVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
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
