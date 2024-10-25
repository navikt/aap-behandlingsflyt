package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import io.ktor.util.reflect.*
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.NavIdent
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.jetbrains.annotations.TestOnly

class AktivitetspliktRepository(private val connection: DBConnection) {

    sealed interface DokumentInput {
        val brudd: Brudd
        val innsender: NavIdent
        val dokumentType: DokumentType
        val begrunnelse: String
    }

    class RegistreringInput(
        override val brudd: Brudd,
        override val innsender: NavIdent,
        override val begrunnelse: String,
        val grunn: Grunn
    ): DokumentInput {
        override val dokumentType = DokumentType.BRUDD
    }

    class FeilregistreringInput(
        override val brudd: Brudd,
        override val innsender: NavIdent,
        override val begrunnelse: String,
    ): DokumentInput {
        override val dokumentType = DokumentType.FEILREGISTRERING
    }

    enum class DokumentType {
        BRUDD,
        FEILREGISTRERING,
    }

    fun lagreBrudd(brudd: List<DokumentInput>): InnsendingId {
        val query = """
            INSERT INTO BRUDD_AKTIVITETSPLIKT
            (SAK_ID, PERIODE, NAV_IDENT, OPPRETTET_TID, HENDELSE_ID, INNSENDING_ID, BRUDD, DOKUMENT_TYPE, BEGRUNNELSE, PARAGRAF, GRUNN)
            VALUES (?, ?::daterange, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        val innsendingId = InnsendingId.ny()

        connection.executeBatch(query, brudd) {
            setParams { request ->
                setLong(1, request.brudd.sakId.toLong())
                setPeriode(2, request.brudd.periode)
                setString(3, request.innsender.navIdent)
                setUUID(4, HendelseId.ny().id)
                setUUID(5, innsendingId.value)
                setEnumName(6, request.brudd.bruddType)
                setEnumName(7, request.dokumentType)
                setString(8, request.begrunnelse)
                setEnumName(9, request.brudd.paragraf)
                when (request) {
                    is RegistreringInput -> {
                        setEnumName(10, request.grunn)
                    }
                    is FeilregistreringInput ->  {
                        setEnumName(10, null)
                    }
                }
            }
        }
        return innsendingId
    }

    fun nyttGrunnlag(behandlingId: BehandlingId, brudd: Set<AktivitetspliktDokument>) {
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
                setLong(2, it.metadata.id.id)
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

    fun hentBrudd(
        brudd: Brudd,
    ): List<AktivitetspliktDokument> {
        val query = """
            |SELECT * FROM BRUDD_AKTIVITETSPLIKT brudd
            | WHERE SAK_ID = ?
            | AND periode = ?
            | AND paragraf = ?
            | AND brudd = ?
            | """.trimMargin()
        return connection.queryList(query) {
            setParams {
                setLong(1, brudd.sakId.toLong())
                setPeriode(2, brudd.periode)
                setEnumName(3, brudd.paragraf)
                setEnumName(4, brudd.bruddType)
            }
            setRowMapper(::mapBruddAktivitetsplikt)
        }
    }

    fun hentBrudd(sakId: SakId): List<AktivitetspliktDokument> {
        val query = """SELECT * FROM BRUDD_AKTIVITETSPLIKT brudd WHERE SAK_ID = ?"""
        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper(::mapBruddAktivitetsplikt)
        }
    }

    fun hentBruddForInnsending(innsendingId: InnsendingId): List<AktivitetspliktDokument> {
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

    private fun mapBruddAktivitetsplikt(row: Row): AktivitetspliktDokument {
        val brudd = Brudd(
            sakId = SakId(row.getLong("SAK_ID")),
            periode = row.getPeriode("PERIODE"),
            bruddType = row.getEnum("BRUDD"),
            paragraf = row.getEnum("PARAGRAF"),
        )

        val metadata = AktivitetspliktDokument.Metadata(
            id = BruddAktivitetspliktId(row.getLong("ID")),
            hendelseId = HendelseId(row.getUUID("HENDELSE_ID")),
            innsendingId = InnsendingId(row.getUUID("INNSENDING_ID")),
            innsender = NavIdent(row.getString("NAV_IDENT")),
            opprettetTid = row.getInstant("OPPRETTET_TID"),
        )
        val begrunnelse = row.getString("BEGRUNNELSE")

        val dokumentType = row.getEnumOrNull("DOKUMENT_TYPE") ?: DokumentType.BRUDD
        return when (dokumentType) {
            DokumentType.BRUDD -> AktivitetspliktRegistrering(
                brudd = brudd,
                metadata = metadata,
                begrunnelse = begrunnelse,
                grunn = row.getEnum("GRUNN")
            )
            DokumentType.FEILREGISTRERING -> AktivitetspliktFeilregistrering(
                brudd = brudd,
                metadata = metadata,
                begrunnelse = begrunnelse,
            )
        }
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
            bruddene.map { it.metadata.id }.toSet()
        }.toSet()
    }
}