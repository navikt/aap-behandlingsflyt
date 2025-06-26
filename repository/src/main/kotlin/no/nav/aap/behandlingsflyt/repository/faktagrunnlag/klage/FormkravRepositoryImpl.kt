package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVarsel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate

class FormkravRepositoryImpl(private val connection: DBConnection) : FormkravRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object : Factory<FormkravRepositoryImpl> {
        override fun konstruer(connection: DBConnection): FormkravRepositoryImpl {
            return FormkravRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): FormkravGrunnlag? {
        val query = """
            SELECT * 
            FROM formkrav_vurdering fv
            LEFT JOIN formkrav_grunnlag fg ON fv.id = fg.vurdering_id
            LEFT JOIN avvist_formkrav_varsel afv ON afv.behandling_id = fg.behandling_id
            WHERE fg.aktiv = true AND fg.behandling_id = ?
            
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapGrunnlag)
        }
    }

    override fun hentAlleVurderinger(sakId: SakId, behandlingId: BehandlingId): Set<FormkravVurdering> {
        TODO("Not yet implemented")
    }

    override fun lagre(behandlingId: BehandlingId, formkravVurdering: FormkravVurdering) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = FormkravGrunnlag(vurdering = formkravVurdering)

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    override fun lagreVarsel(behandlingId: BehandlingId, varsel: BrevbestillingReferanse) {
        val query = """
            INSERT INTO avvist_formkrav_varsel (behandling_id, brev_referanse) 
            VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setUUID(2, varsel.brevbestillingReferanse)
            }
        }
    }

    override fun lagreFrist(behandlingId: BehandlingId, datoVarslet: LocalDate, svarfrist: LocalDate) {
        val query = """
            UPDATE avvist_formkrav_varsel SET dato_varslet=?, frist=? WHERE behandling_id=?
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLocalDate(1, datoVarslet)
                setLocalDate(2, svarfrist)
                setLong(3, behandlingId.toLong())
            }
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: FormkravGrunnlag) {
        val vurderingId = lagreVurdering(nyttGrunnlag.vurdering)
        val query = """
            INSERT INTO FORMKRAV_GRUNNLAG (BEHANDLING_ID, VURDERING_ID, AKTIV) 
            VALUES (?, ?, TRUE)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    private fun lagreVurdering(vurdering: FormkravVurdering): Long {
        val query = """
            INSERT INTO FORMKRAV_VURDERING 
            (BEGRUNNELSE, ER_BRUKER_PART, ER_FRIST_OVERHOLDT, ER_KONKRET, ER_SIGNERT, VURDERT_AV, LIKEVEL_BEHANDLES) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erBrukerPart)
                setBoolean(3, vurdering.erFristOverholdt)
                setBoolean(4, vurdering.erKonkret)
                setBoolean(5, vurdering.erSignert)
                setString(6, vurdering.vurdertAv)
                setBoolean(7, vurdering.likevelBehandles)
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // TODO: Avklar om vi trenger flere behandlinger per klage
        // Gjør ingenting
    }

    override fun slett(behandlingId: BehandlingId) {
        val vurderingIds = getIdForVurderingerForGrunnlaget(behandlingId)

        val deletedRowsGrunnlag = connection.executeReturnUpdated("""
            delete from formkrav_grunnlag where behandling_id = ?; 
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
            }
        }

        val deletedRowsVurdering = connection.executeReturnUpdated("""
            delete from formkrav_vurdering where id = ANY(?::bigint[]);
        """.trimIndent()) {
            setParams {
                setLongArray(1, vurderingIds)
            }
        }

        log.info("Slettet $deletedRowsGrunnlag rader fra formkrav_grunnlag og $deletedRowsVurdering rader fra formkrav_vurdering")
    }

    private fun getIdForVurderingerForGrunnlaget(behandlingId: BehandlingId): List<Long> =
        connection.queryList(
            """
                SELECT vurdering_id
                FROM formkrav_grunnlag
                WHERE behandling_id = ? AND vurdering_id is not null
                """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row ->
                row.getLong("vurdering_id")
            }
        }

    private fun mapGrunnlag(row: Row): FormkravGrunnlag {
        return FormkravGrunnlag(
            vurdering = mapFormkravVurdering(row),
            varsel = mapFormkravVarsel(row)
        )
    }

    private fun mapFormkravVarsel(row: Row): FormkravVarsel? {
        var varselUuid = row.getUUIDOrNull("brev_referanse")

        return varselUuid?.let {
            FormkravVarsel(
                varselId = BrevbestillingReferanse(varselUuid),
                svarfrist = row.getLocalDateOrNull("frist"),
                sendtDato = row.getLocalDateOrNull("dato_varslet")
            )
        }
    }

    private fun mapFormkravVurdering(row: Row): FormkravVurdering {
        return FormkravVurdering(
            begrunnelse = row.getString("BEGRUNNELSE"),
            erBrukerPart = row.getBoolean("ER_BRUKER_PART"),
            erFristOverholdt = row.getBoolean("ER_FRIST_OVERHOLDT"),
            erKonkret = row.getBoolean("ER_KONKRET"),
            erSignert = row.getBoolean("ER_SIGNERT"),
            vurdertAv = row.getString("VURDERT_AV"),
            likevelBehandles = row.getBooleanOrNull("LIKEVEL_BEHANDLES"),
        )
    }


    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE FORMKRAV_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }
}