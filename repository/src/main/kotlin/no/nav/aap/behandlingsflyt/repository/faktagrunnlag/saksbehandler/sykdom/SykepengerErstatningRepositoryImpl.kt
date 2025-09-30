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

    override fun lagre(behandlingId: BehandlingId, vurdering: SykepengerVurdering?) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        val nyttGrunnlag = SykepengerErstatningGrunnlag(vurdering = vurdering)

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            lagreGrunnlag(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagreGrunnlag(behandlingId: BehandlingId, nyttGrunnlag: SykepengerErstatningGrunnlag) {
        val vurdering: SykepengerVurdering? = nyttGrunnlag.vurdering
        var vurderingId: Long? = null
        var key: Long? = null
        if (vurdering != null) {
            val insert = """
                INSERT INTO SYKEPENGE_VURDERINGER DEFAULT VALUES;
            """.trimIndent()
            key = connection.executeReturnKey(insert)


            val query = """
            INSERT INTO SYKEPENGE_VURDERING (begrunnelse, oppfylt, grunn, vurdert_av, vurderinger_id) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

            vurderingId = connection.executeReturnKey(query) {
                setParams {
                    setString(1, vurdering.begrunnelse)
                    setBoolean(2, vurdering.harRettPå)
                    setEnumName(3, vurdering.grunn)
                    setString(4, vurdering.vurdertAv)
                    setLong(5, key)
                }
            }

            vurdering.dokumenterBruktIVurdering.forEach {
                lagreDokument(vurderingId, it)
            }
        }
        val grunnlagQuery = """
             INSERT INTO SYKEPENGE_ERSTATNING_GRUNNLAG (behandling_id, vurdering_id, vurderinger_id) VALUES (?, ?, ?)
        """.trimIndent()

        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
                setLong(3, key)
            }
        }
    }

    private fun lagreDokument(vurderingId: Long, journalpostId: JournalpostId) {
        val query = """
            INSERT INTO SYKEPENGE_VURDERING_DOKUMENTER (vurdering_id, journalpost) 
            VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
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
        val hentHvisEksisterer = hentHvisEksisterer(fraBehandling)
        if (hentHvisEksisterer == null) {
            return
        }

        val query = """
            INSERT INTO SYKEPENGE_ERSTATNING_GRUNNLAG (behandling_id, vurdering_id, vurderinger_id) 
            SELECT ?, vurdering_id, vurderinger_id from SYKEPENGE_ERSTATNING_GRUNNLAG where behandling_id = ? and aktiv
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
                SykepengerErstatningGrunnlag(row.getLong("id"), row.getLongOrNull("vurdering_id")?.let(::mapVurdering))
            }
        }
    }

    private fun mapVurdering(vurderingId: Long): SykepengerVurdering? {
        val query = """
            SELECT * FROM SYKEPENGE_VURDERING WHERE id = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper { row ->
                SykepengerVurdering(
                    begrunnelse = row.getString("begrunnelse"),
                    dokumenterBruktIVurdering = hentDokumenter(vurderingId),
                    harRettPå = row.getBoolean("oppfylt"),
                    grunn = row.getEnumOrNull("grunn"),
                    vurdertAv = row.getString("vurdert_av"),
                    vurdertTidspunkt = row.getLocalDateTime("opprettet_tid"),
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


    override fun hent(behandlingId: BehandlingId): SykepengerErstatningGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
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
                    SELECT vurdering_id
                    FROM sykepenge_erstatning_grunnlag
                    WHERE behandling_id = ? AND vurdering_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurdering_id")
        }
    }

}
