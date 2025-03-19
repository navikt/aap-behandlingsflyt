package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory

class BistandRepositoryImpl(private val connection: DBConnection) : BistandRepository {

    companion object : Factory<BistandRepositoryImpl> {
        override fun konstruer(connection: DBConnection): BistandRepositoryImpl {
            return BistandRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): BistandGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT ID, BISTAND_ID, BISTAND_VURDERINGER_ID
            FROM BISTAND_GRUNNLAG
            WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                BistandGrunnlag(
                    id = row.getLong("ID"),
                    vurderinger = mapVurderingerTemp(
                        row.getLongOrNull("BISTAND_ID"),
                        row.getLongOrNull("BISTAND_VURDERINGER_ID")
                    )
                )
            }
        }
    }
    
    private fun mapVurderingerTemp(bistandId: Long?, bistandsvurderingerId: Long?): List<BistandVurdering> {
        if (bistandsvurderingerId == null) {
            return listOfNotNull(mapBistandsvurdering(bistandId))
        } else {
            return mapBistandsvurderinger(bistandsvurderingerId)
        }

    }
    
    private fun mapBistandsvurdering(bistandvurderingId: Long?): BistandVurdering?{
        if (bistandvurderingId == null) {
            return null
        }
        
        return connection.queryFirstOrNull(
            """
            SELECT * FROM bistand WHERE ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, bistandvurderingId)
            }
            setRowMapper(::bistandvurderingRowMapper)
        }
        
    }

    private fun mapBistandsvurderinger(bistandsvurderingerId: Long?): List<BistandVurdering> {
        return connection.queryList(
            """
                SELECT * FROM bistand WHERE BISTAND_VURDERINGER_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, bistandsvurderingerId)
            }
            setRowMapper(::bistandvurderingRowMapper)
        }
    }

    private fun bistandvurderingRowMapper(row: Row): BistandVurdering {
        return BistandVurdering(
            begrunnelse = row.getString("BEGRUNNELSE"),
            erBehovForAktivBehandling = row.getBoolean("BEHOV_FOR_AKTIV_BEHANDLING"),
            erBehovForArbeidsrettetTiltak = row.getBoolean("BEHOV_FOR_ARBEIDSRETTET_TILTAK"),
            erBehovForAnnenOppfølging = row.getBooleanOrNull("BEHOV_FOR_ANNEN_OPPFOELGING"),
            vurderingenGjelderFra = row.getLocalDateOrNull("VURDERINGEN_GJELDER_FRA"),
            skalVurdereAapIOvergangTilUføre = row.getBooleanOrNull("OVERGANG_TIL_UFOERE"),
            skalVurdereAapIOvergangTilArbeid = row.getBooleanOrNull("OVERGANG_TIL_ARBEID"),
            overgangBegrunnelse = row.getStringOrNull("OVERGANG_BEGRUNNELSE"),
            vurdertAv = row.getString("VURDERT_AV")
        )
    }

    override fun hentHistoriskeBistandsvurderinger(sakId: SakId, behandlingId: BehandlingId): List<BistandVurdering> {
        val query = """
            SELECT DISTINCT bistand.*
            FROM bistand_grunnlag grunnlag
            INNER JOIN bistand ON grunnlag.bistand_id = bistand.id
            INNER JOIN behandling ON grunnlag.behandling_id = behandling.id
            WHERE grunnlag.aktiv AND behandling.sak_id = ?
                AND behandling.opprettet_tid < (select a.opprettet_tid from behandling a where id = ?)
            ORDER BY bistand.opprettet_tid
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.id)
                setLong(2, behandlingId.id)
            }
            setRowMapper(::bistandvurderingRowMapper)
        }
    }

    override fun lagre(behandlingId: BehandlingId, bistandsvurderinger: List<BistandVurdering>) {
        val eksisterendeBistandGrunnlag = hentHvisEksisterer(behandlingId)

        val nyttGrunnlag = BistandGrunnlag(
            id = null,
            vurderinger = bistandsvurderinger
        )

        if (eksisterendeBistandGrunnlag != nyttGrunnlag) {
            eksisterendeBistandGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: BistandGrunnlag) {
        //val bistandvurderingerId = lagreBistandsvurderinger(nyttGrunnlag.vurderinger)
        val (bistandId, bistandvurderingerId) = lagreBistandsvurdering(nyttGrunnlag.vurderinger.firstOrNull()) // TODO: Bruk den over ved konstruksjon av tidlslinje

        connection.execute("INSERT INTO BISTAND_GRUNNLAG (BEHANDLING_ID, BISTAND_ID, BISTAND_VURDERINGER_ID) VALUES (?, ?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, bistandId)
                setLong(3, bistandvurderingerId)
            }
        }
    }

    // Double write frem til migrering av data
    private fun lagreBistandsvurdering(vurdering: BistandVurdering?): Pair<Long?, Long> {
        val bistandvurderingerId = connection.executeReturnKey("""INSERT INTO BISTAND_VURDERINGER DEFAULT VALUES""")

        if (vurdering == null) {
            return Pair(null, bistandvurderingerId)
        }

        val id = connection.executeReturnKey(
            "INSERT INTO BISTAND (BEGRUNNELSE, BEHOV_FOR_AKTIV_BEHANDLING, BEHOV_FOR_ARBEIDSRETTET_TILTAK, BEHOV_FOR_ANNEN_OPPFOELGING, VURDERINGEN_GJELDER_FRA, VURDERT_AV, OVERGANG_BEGRUNNELSE, OVERGANG_TIL_UFOERE, OVERGANG_TIL_ARBEID, BISTAND_VURDERINGER_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        ) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erBehovForAktivBehandling)
                setBoolean(3, vurdering.erBehovForArbeidsrettetTiltak)
                setBoolean(4, vurdering.erBehovForAnnenOppfølging)
                setLocalDate(5, vurdering.vurderingenGjelderFra)
                setString(6, vurdering.vurdertAv)
                setString(7, vurdering.overgangBegrunnelse)
                setBoolean(8, vurdering.skalVurdereAapIOvergangTilUføre)
                setBoolean(9, vurdering.skalVurdereAapIOvergangTilArbeid)
                setLong(10, bistandvurderingerId)
            }
        }

        return Pair(id, bistandvurderingerId)
    }


    private fun lagreBistandsvurderinger(vurderinger: List<BistandVurdering>): Long {
        val bistandvurderingerId = connection.executeReturnKey("""INSERT INTO BISTAND_VURDERINGER DEFAULT VALUES""")

        connection.executeBatch(
            "INSERT INTO BISTAND (BEGRUNNELSE, BEHOV_FOR_AKTIV_BEHANDLING, BEHOV_FOR_ARBEIDSRETTET_TILTAK, BEHOV_FOR_ANNEN_OPPFOELGING, VURDERINGEN_GJELDER_FRA, VURDERT_AV, OVERGANG_BEGRUNNELSE, OVERGANG_TIL_UFOERE, OVERGANG_TIL_ARBEID, BISTAND_VURDERINGER_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            vurderinger
        ) {
            setParams { vurdering ->
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erBehovForAktivBehandling)
                setBoolean(3, vurdering.erBehovForArbeidsrettetTiltak)
                setBoolean(4, vurdering.erBehovForAnnenOppfølging)
                setLocalDate(5, vurdering.vurderingenGjelderFra)
                setString(6, vurdering.vurdertAv)
                setString(7, vurdering.overgangBegrunnelse)
                setBoolean(8, vurdering.skalVurdereAapIOvergangTilUføre)
                setBoolean(9, vurdering.skalVurdereAapIOvergangTilArbeid)
                setLong(10, bistandvurderingerId)
            }
        }

        return bistandvurderingerId
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE BISTAND_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute(
            """
            INSERT INTO BISTAND_GRUNNLAG (BEHANDLING_ID, BISTAND_ID, BISTAND_VURDERINGER_ID) 
            SELECT ?, BISTAND_ID, BISTAND_VURDERINGER_ID 
            FROM BISTAND_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}