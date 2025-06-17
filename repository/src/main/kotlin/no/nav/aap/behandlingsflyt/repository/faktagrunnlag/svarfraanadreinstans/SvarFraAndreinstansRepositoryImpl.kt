package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.svarfraanadreinstans

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory

class SvarFraAndreinstansRepositoryImpl(private val connection: DBConnection) : SvarFraAndreinstansRepository {
    companion object : Factory<SvarFraAndreinstansRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SvarFraAndreinstansRepositoryImpl {
            return SvarFraAndreinstansRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SvarFraAndreinstansGrunnlag? {
        val query = """
            SELECT * FROM SVAR_FRA_ANDREINSTANS_VURDERING
            WHERE id IN (
                SELECT vurdering_id FROM SVAR_FRA_ANDREINSTANS_GRUNNLAG
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

    private fun mapGrunnlag(row: Row): SvarFraAndreinstansGrunnlag {
        return SvarFraAndreinstansGrunnlag(
            vurdering = mapSvarFraAndreinstansVurdering(row),
        )
    }

    private fun mapSvarFraAndreinstansVurdering(row: Row): SvarFraAndreinstansVurdering {
        return SvarFraAndreinstansVurdering(
            begrunnelse = row.getString("BEGRUNNELSE"),
            konsekvens = row.getEnum("KONSEKVENS"),
            vilkårSomOmgjøres = row.getArray("vilkaar_som_skal_omgjoeres", String::class)
                .mapNotNull { Hjemmel.fraHjemmel(it) },
            vurdertAv = row.getString("VURDERT_AV"),
            opprettet = row.getInstant("opprettet_tid")
        )
    }


    override fun lagre(
        behandlingId: BehandlingId,
        svarFraAndreinstansVurdering: SvarFraAndreinstansVurdering
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = SvarFraAndreinstansGrunnlag(vurdering = svarFraAndreinstansVurdering)
        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }


    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: SvarFraAndreinstansGrunnlag) {
        val vurderingId = lagreVurdering(nyttGrunnlag.vurdering)
        val query = """
            INSERT INTO SVAR_FRA_ANDREINSTANS_GRUNNLAG (BEHANDLING_ID, VURDERING_ID, AKTIV) 
            VALUES (?, ?, TRUE)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    private fun lagreVurdering(vurdering: SvarFraAndreinstansVurdering): Long {
        val query = """
            INSERT INTO SVAR_FRA_ANDREINSTANS_VURDERING
            (BEGRUNNELSE, KONSEKVENS, VILKAAR_SOM_SKAL_OMGJOERES, VURDERT_AV)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setEnumName(2, vurdering.konsekvens)
                setArray(3, vurdering.vilkårSomOmgjøres.map { it.hjemmel })
                setString(4, vurdering.vurdertAv)
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE SVAR_FRA_ANDREINSTANS_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        // Skal man kunne trekke klage etter den er oversendt til Kabal?
    }


}