package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.lookup.repository.Factory
import org.jetbrains.annotations.TestOnly

class AktivitetspliktRepositoryImpl(private val connection: DBConnection) : AktivitetspliktRepository {

    companion object : Factory<AktivitetspliktRepositoryImpl> {
        override fun konstruer(connection: DBConnection): AktivitetspliktRepositoryImpl {
            return AktivitetspliktRepositoryImpl(connection)
        }
    }

    override fun lagreBrudd(sakId: SakId, brudd: List<DokumentInput>): InnsendingId {
        val query = """
            INSERT INTO BRUDD_AKTIVITETSPLIKT
            (SAK_ID, PERIODE, NAV_IDENT, OPPRETTET_TID, HENDELSE_ID, INNSENDING_ID, BRUDD, DOKUMENT_TYPE, BEGRUNNELSE, PARAGRAF, GRUNN)
            VALUES (?, ?::daterange, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        val innsendingId = InnsendingId.ny()

        connection.executeBatch(query, brudd) {
            setParams { request ->
                setLong(1, sakId.toLong())
                setPeriode(2, request.brudd.periode)
                setString(3, request.innsender.ident)
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

                    is FeilregistreringInput -> {
                        setEnumName(10, null)
                    }
                }
            }
        }
        return innsendingId
    }

    override fun nyttGrunnlag(behandlingId: BehandlingId, brudd: Set<AktivitetspliktDokument>) {
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

    override fun hentGrunnlagHvisEksisterer(behandlingId: BehandlingId): AktivitetspliktGrunnlag? {
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

    override fun slett(behandlingId: BehandlingId) {
        connection.execute(
            """
            delete from brudd_aktivitetsplikt where brudd_aktivitetsplikt.id in (
                select brudd_aktivitetsplikt_id from brudd_aktivitetsplikter 
                join brudd_aktivitetsplikt_grunnlag on brudd_aktivitetsplikter.brudd_aktivitetsplikt_grunnlag_id = brudd_aktivitetsplikt_grunnlag.id
                where brudd_aktivitetsplikt_grunnlag.behandling_id = ?1
                ); 
            delete from brudd_aktivitetsplikter where brudd_aktivitetsplikter.brudd_aktivitetsplikt_grunnlag_id in (
                select id from brudd_aktivitetsplikt_grunnlag where behandling_id = ?1
                )
            ; 
            delete from brudd_aktivitetsplikt_grunnlag where behandling_id = ?1;
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
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

    @TestOnly
    internal fun hentBrudd(
        sakId: SakId,
        brudd: Brudd,
    ): List<AktivitetspliktDokument> {
        val query = """
            |SELECT * FROM BRUDD_AKTIVITETSPLIKT brudd
            | WHERE SAK_ID = ?
            | AND periode = ?::daterange
            | AND paragraf = ?
            | AND brudd = ?
            | """.trimMargin()
        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
                setPeriode(2, brudd.periode)
                setEnumName(3, brudd.paragraf)
                setEnumName(4, brudd.bruddType)
            }
            setRowMapper(::mapBruddAktivitetsplikt)
        }
    }

    override fun hentBrudd(sakId: SakId): List<AktivitetspliktDokument> {
        val query = """SELECT * FROM BRUDD_AKTIVITETSPLIKT brudd WHERE SAK_ID = ?"""
        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper(::mapBruddAktivitetsplikt)
        }
    }

    override fun hentBrudd(bruddAktivitetspliktId: BruddAktivitetspliktId): AktivitetspliktDokument {
        val query = """SELECT * FROM BRUDD_AKTIVITETSPLIKT brudd WHERE ID = ?"""
        return connection.queryFirst(query) {
            setParams { setLong(1, bruddAktivitetspliktId.id) }
            setRowMapper(::mapBruddAktivitetsplikt)
        }
    }

    override fun hentBruddForInnsending(innsendingId: InnsendingId): List<AktivitetspliktDokument> {
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

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentGrunnlagHvisEksisterer(fraBehandling) ?: return
        nyttGrunnlag(tilBehandling, eksisterendeGrunnlag.bruddene)
    }

    private fun mapBruddAktivitetsplikt(row: Row): AktivitetspliktDokument {
        val brudd = Brudd(
            periode = row.getPeriode("PERIODE"),
            bruddType = row.getEnum("BRUDD"),
            paragraf = row.getEnum("PARAGRAF"),
        )

        val metadata = AktivitetspliktDokument.Metadata(
            id = BruddAktivitetspliktId(row.getLong("ID")),
            hendelseId = HendelseId(row.getUUID("HENDELSE_ID")),
            innsendingId = InnsendingId(row.getUUID("INNSENDING_ID")),
            innsender = Bruker(row.getString("NAV_IDENT")),
            opprettetTid = row.getInstant("OPPRETTET_TID"),
        )
        val begrunnelse = row.getString("BEGRUNNELSE")

        val dokumentType = row.getEnumOrNull("DOKUMENT_TYPE") ?: DokumentType.REGISTRERING
        return when (dokumentType) {
            DokumentType.REGISTRERING -> AktivitetspliktRegistrering(
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