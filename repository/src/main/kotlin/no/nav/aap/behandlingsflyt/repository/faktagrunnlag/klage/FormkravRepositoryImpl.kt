package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory

class FormkravRepositoryImpl(private val connection: DBConnection) : FormkravRepository {

    companion object : Factory<FormkravRepositoryImpl> {
        override fun konstruer(connection: DBConnection): FormkravRepositoryImpl {
            return FormkravRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): FormkravGrunnlag? {
        val query = """
            SELECT * FROM FORMKRAV_VURDERING
            WHERE id IN (
                SELECT vurdering_id FROM FORMKRAV_GRUNNLAG
                WHERE BEHANDLING_ID = ? AND AKTIV = TRUE
            )
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

    override fun slett(fraBehandling: BehandlingId) {
        // Gjør ingenting
    }

    private fun mapGrunnlag(row: Row): FormkravGrunnlag {
        return FormkravGrunnlag(
            vurdering = mapFormkravVurdering(row),
        )
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