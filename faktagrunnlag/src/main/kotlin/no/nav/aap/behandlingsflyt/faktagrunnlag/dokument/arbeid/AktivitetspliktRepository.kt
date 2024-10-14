package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.jetbrains.annotations.TestOnly

class AktivitetspliktRepository(private val connection: DBConnection) {
    class LagreBruddInput(
        val sakId: SakId,
        val navIdent: NavIdent,
        val brudd: BruddAktivitetsplikt.Type,
        val paragraf: BruddAktivitetsplikt.Paragraf,
        val begrunnelse: String,
        val periode: Periode,
    )

    fun lagreBrudd(brudd: List<LagreBruddInput>): InnsendingId {
        val query = """
            INSERT INTO BRUDD_AKTIVITETSPLIKT
            (SAK_ID, BRUDD, PERIODE, BEGRUNNELSE, PARAGRAF, NAV_IDENT, OPPRETTET_TID, HENDELSE_ID, INNSENDING_ID ) 
            VALUES (?, ?, ?::daterange, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?)
            """.trimIndent()

        val innsendingId = InnsendingId.ny()

        connection.executeBatch(query, brudd) {
            setParams { request ->
                setLong(1, request.sakId.toLong())
                setEnumName(2, request.brudd)
                setPeriode(3, request.periode)
                setString(4, request.begrunnelse)
                setEnumName(5, request.paragraf)
                setString(6, request.navIdent.navIdent)
                setUUID(7, HendelseId.ny().id)
                setUUID(8, innsendingId.value)
            }
        }
        return innsendingId
    }

    fun nyttGrunnlag(behandlingId: BehandlingId, brudd: Set<BruddAktivitetsplikt>) {
        val eksisterendeGrunnlag = this.hentGrunnlagHvisEksisterer(behandlingId)
        val eksisterendeBrudd = eksisterendeGrunnlag?.bruddene ?: emptyList()

        if (brudd == eksisterendeBrudd) {
            return
        }

        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }
        val insertGrunnlag = """
            INSERT INTO BRUDD_AKTIVITETSPLIKT_GRUNNLAG (BEHANDLING_ID) VALUES (?)
        """.trimIndent()
        val grunnlagId = connection.executeReturnKey(insertGrunnlag) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        val insertAktivitetskorteneQuery = """
            INSERT INTO BRUDD_AKTIVITETSPLIKTER (BRUDD_AKTIVITETSPLIKT_GRUNNLAG_ID, BRUDD_AKTIVITETSPLIKT_ID)
            VALUES (?, ?)
            """.trimIndent()
        connection.executeBatch(insertAktivitetskorteneQuery, brudd) {
            setParams {
                setLong(1, grunnlagId)
                setLong(2, it.id.id)
            }
        }
    }

    fun hentGrunnlagHvisEksisterer(behandlingId: BehandlingId): AktivitetspliktGrunnlag? {
        val grunnlagId = finnGrunnlagId(behandlingId) ?: return null

        val query = """
            SELECT brudd.*
            FROM BRUDD_AKTIVITETSPLIKT brudd 
            INNER JOIN BRUDD_AKTIVITETSPLIKTER snapshot ON brudd.id = snapshot.brudd_aktivitetsplikt_id
            WHERE snapshot.brudd_aktivitetsplikt_grunnlag_id = ?
        """.trimIndent()
        val bruddene = connection.querySet(query) {
            setParams {
                setLong(1, grunnlagId)
            }
            setRowMapper(::mapBruddAktivitetsplikt)
        }
        return AktivitetspliktGrunnlag(
            bruddene = bruddene,
        )
    }

    private fun finnGrunnlagId(behandlingId: BehandlingId): Long? =
        connection.queryFirstOrNull(
            """
            SELECT id FROM BRUDD_AKTIVITETSPLIKT_GRUNNLAG where behandling_id = ? and aktiv = true
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                it.getLong("id")
            }
        }

    fun hentBrudd(sakId: SakId): List<BruddAktivitetsplikt> {
        val query = """SELECT * FROM BRUDD_AKTIVITETSPLIKT brudd WHERE SAK_ID = ?"""
        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper(::mapBruddAktivitetsplikt)
        }
    }

    fun hentBruddForInnsending(innsendingId: InnsendingId): List<BruddAktivitetsplikt> {
        val query = """
            SELECT *
            FROM BRUDD_AKTIVITETSPLIKT brudd
            WHERE innsending_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setUUID(1, innsendingId.value)
            }
            setRowMapper(::mapBruddAktivitetsplikt)
        }
    }

    fun kopier(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        val eksisterendeGrunnlag = hentGrunnlagHvisEksisterer(fraBehandlingId) ?: return
        nyttGrunnlag(tilBehandlingId, eksisterendeGrunnlag.bruddene)
    }

    fun deleteAll() {
        connection.execute(
            """
                DELETE FROM BRUDD_AKTIVITETSPLIKT
            """.trimIndent()
        ) {}
    }

    private fun mapBruddAktivitetsplikt(row: Row): BruddAktivitetsplikt {
        return BruddAktivitetsplikt(
            id = BruddAktivitetspliktId(row.getLong("ID")),
            type = row.getEnum("BRUDD"),
            paragraf = row.getEnum("PARAGRAF"),
            periode = row.getPeriode("PERIODE"),
            begrunnelse = row.getString("BEGRUNNELSE"),
            sakId = SakId(row.getLong("SAK_ID")),
            navIdent = NavIdent(row.getString("NAV_IDENT")),
            hendelseId = HendelseId(row.getUUID("HENDELSE_ID")),
            innsendingId = InnsendingId(row.getUUID("INNSENDING_ID")),
            opprettetTid = row.getInstant("OPPRETTET_TID"),
        )
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute(
            """UPDATE BRUDD_AKTIVITETSPLIKT_GRUNNLAG set AKTIV = false WHERE BEHANDLING_ID = ? and AKTIV = true"""
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    @TestOnly
    internal fun hentAlleGrunnlagKunTestIkkeProd(behandlingId: BehandlingId): Set<Set<BruddAktivitetspliktId>> {
        val grunnlagIder = connection.queryList(
            """
            SELECT id FROM BRUDD_AKTIVITETSPLIKT_GRUNNLAG where behandling_id = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                it.getLong("id")
            }
        }

        return grunnlagIder.map { grunnlagId ->
            val query = """
                SELECT brudd.*
                FROM BRUDD_AKTIVITETSPLIKT brudd 
                INNER JOIN BRUDD_AKTIVITETSPLIKTER snapshot ON brudd.id = snapshot.brudd_aktivitetsplikt_id
                WHERE snapshot.brudd_aktivitetsplikt_grunnlag_id = ?
            """.trimIndent()
            val bruddene = connection.querySet(query) {
                setParams {
                    setLong(1, grunnlagId)
                }
                setRowMapper {
                    mapBruddAktivitetsplikt(it)
                }
            }
            bruddene.map { it.id }.toSet()
        }.toSet()
    }
}