package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory

class BehandlendeEnhetRepositoryImpl(private val connection: DBConnection) : BehandlendeEnhetRepository {

    companion object : Factory<BehandlendeEnhetRepositoryImpl> {
        override fun konstruer(connection: DBConnection): BehandlendeEnhetRepositoryImpl {
            return BehandlendeEnhetRepositoryImpl(connection)
        }
    }
    
    override fun hentHvisEksisterer(behandlingId: BehandlingId): BehandlendeEnhetGrunnlag? {
        val query = """
            SELECT * FROM BEHANDLENDE_ENHET_VURDERING
            WHERE id IN (
                SELECT vurdering_id FROM BEHANDLENDE_ENHET_GRUNNLAG
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

    override fun lagre(behandlingId: BehandlingId, behandlendeEnhetVurdering: BehandlendeEnhetVurdering) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = BehandlendeEnhetGrunnlag(vurdering = behandlendeEnhetVurdering)
        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Skal ikke kopieres
    }

    override fun slett(behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: BehandlendeEnhetGrunnlag) {
        val vurderingId = lagreVurdering(nyttGrunnlag.vurdering)
        val query = """
            INSERT INTO BEHANDLENDE_ENHET_GRUNNLAG (BEHANDLING_ID, VURDERING_ID, AKTIV) 
            VALUES (?, ?, TRUE)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    private fun lagreVurdering(vurdering: BehandlendeEnhetVurdering): Long {
        val query = """
            INSERT INTO BEHANDLENDE_ENHET_VURDERING 
            (skal_behandles_av_nay, skal_behandles_av_kontor, vurdert_av) 
            VALUES (?, ?, ?)
        """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setBoolean(1, vurdering.skalBehandlesAvNay)
                setBoolean(2, vurdering.skalBehandlesAvKontor)
                setString(3, vurdering.vurdertAv)
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    private fun mapGrunnlag(row: Row): BehandlendeEnhetGrunnlag {
        return BehandlendeEnhetGrunnlag(
            vurdering = mapBehandlendeEnhetVurdering(row),
        )
    }

    private fun mapBehandlendeEnhetVurdering(row: Row): BehandlendeEnhetVurdering {
        return BehandlendeEnhetVurdering(
            skalBehandlesAvNay = row.getBoolean("skal_behandles_av_nay"),
            skalBehandlesAvKontor = row.getBoolean("skal_behandles_av_kontor"),
            vurdertAv = row.getString("vurdert_av"),
            opprettet = row.getInstant("opprettet_tid")
        )
    }


    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE BEHANDLENDE_ENHET_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }
}