package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.jetbrains.annotations.TestOnly

class AktivitetspliktRepository(private val connection: DBConnection) {
    class DokumentInput(
        val sakId: SakId,
        val dokumenttype: BruddAktivitetsplikt.Dokumenttype,
        val innsender: NavIdent,
        val periode: Periode,
        val brudd: BruddAktivitetsplikt.Brudd,
        val paragraf: BruddAktivitetsplikt.Paragraf,
        val begrunnelse: String,
        val grunn: BruddAktivitetsplikt.Grunn
    )

    fun lagreBrudd(brudd: List<DokumentInput>): InnsendingId {
        val query = """
            INSERT INTO BRUDD_AKTIVITETSPLIKT
            (SAK_ID, PERIODE, NAV_IDENT, OPPRETTET_TID, HENDELSE_ID, INNSENDING_ID, DOKUMENT_TYPE, BRUDD, BEGRUNNELSE, PARAGRAF, GRUNN)
            VALUES (?, ?::daterange, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        val innsendingId = InnsendingId.ny()

        connection.executeBatch(query, brudd) {
            setParams { request ->
                setLong(1, request.sakId.toLong())
                setPeriode(2, request.periode)
                setString(3, request.innsender.navIdent)
                setUUID(4, HendelseId.ny().id)
                setUUID(5, innsendingId.value)
                setEnumName(6, request.dokumenttype)
                setEnumName(7, request.brudd)
                setString(8, request.begrunnelse)
                setEnumName(9, request.paragraf)
                setEnumName(10, request.grunn)
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
        val id = BruddAktivitetspliktId(row.getLong("ID"))
        val periode = row.getPeriode("PERIODE")
        val opprettetTid = row.getInstant("OPPRETTET_TID")
        val sakId = SakId(row.getLong("SAK_ID"))
        val navIdent = NavIdent(row.getString("NAV_IDENT"))
        val hendelseId = HendelseId(row.getUUID("HENDELSE_ID"))
        val innsendingId = InnsendingId(row.getUUID("INNSENDING_ID"))

        return BruddAktivitetsplikt(
            id = id,
            periode = periode,
            sakId = sakId,
            innsender = navIdent,
            hendelseId = hendelseId,
            innsendingId = innsendingId,
            opprettetTid = opprettetTid,
            brudd = row.getEnum("BRUDD"),
            paragraf = row.getEnum("PARAGRAF"),
            begrunnelse = row.getString("BEGRUNNELSE"),
            dokumenttype = row.getEnumOrNull("DOKUMENT_TYPE") ?: BruddAktivitetsplikt.Dokumenttype.BRUDD,
            grunn = row.getEnum("GRUNN")
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