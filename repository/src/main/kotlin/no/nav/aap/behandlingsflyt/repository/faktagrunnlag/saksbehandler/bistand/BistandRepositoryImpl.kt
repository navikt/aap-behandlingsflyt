package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class BistandRepositoryImpl(private val connection: DBConnection) : BistandRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<BistandRepositoryImpl> {
        override fun konstruer(connection: DBConnection): BistandRepositoryImpl {
            return BistandRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): BistandGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT ID, BISTAND_VURDERINGER_ID
            FROM BISTAND_GRUNNLAG
            WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                BistandGrunnlag(
                    vurderinger = mapBistandsvurderinger(row.getLongOrNull("BISTAND_VURDERINGER_ID"))
                )
            }
        }
    }

    private fun mapBistandsvurderinger(bistandsvurderingerId: Long?): List<Bistandsvurdering> {
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

    private fun bistandvurderingRowMapper(row: Row): Bistandsvurdering {
        return Bistandsvurdering(
            begrunnelse = row.getString("BEGRUNNELSE"),
            erBehovForAktivBehandling = row.getBoolean("BEHOV_FOR_AKTIV_BEHANDLING"),
            erBehovForArbeidsrettetTiltak = row.getBoolean("BEHOV_FOR_ARBEIDSRETTET_TILTAK"),
            erBehovForAnnenOppfølging = row.getBooleanOrNull("BEHOV_FOR_ANNEN_OPPFOELGING"),
            vurderingenGjelderFra = row.getLocalDate("VURDERINGEN_GJELDER_FRA"),
            skalVurdereAapIOvergangTilArbeid = row.getBooleanOrNull("OVERGANG_TIL_ARBEID"),
            overgangBegrunnelse = row.getStringOrNull("OVERGANG_BEGRUNNELSE"),
            vurdertAv = row.getString("VURDERT_AV"),
            opprettet = row.getInstant("OPPRETTET_TID"),
            vurdertIBehandling = row.getLong("VURDERT_I_BEHANDLING").let(::BehandlingId),
        )
    }

    override fun lagre(behandlingId: BehandlingId, bistandsvurderinger: List<Bistandsvurdering>) {
        val eksisterendeBistandGrunnlag = hentHvisEksisterer(behandlingId)

        val nyttGrunnlag = BistandGrunnlag(
            vurderinger = bistandsvurderinger
        )

        if (eksisterendeBistandGrunnlag != nyttGrunnlag) {
            eksisterendeBistandGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val bistandVurderingerIds = getBistandVurderingerIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from bistand_grunnlag where behandling_id = ?; 
            delete from bistand where bistand_vurderinger_id = ANY(?::bigint[]);
            delete from bistand_vurderinger where id = ANY(?::bigint[]);
           
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, bistandVurderingerIds)
                setLongArray(3, bistandVurderingerIds)
            }
        }
        log.info("Slettet $deletedRows rader fra bistand_grunnlag")
    }

    private fun getBistandVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT bistand_vurderinger_id
                    FROM bistand_grunnlag
                    WHERE behandling_id = ? AND bistand_vurderinger_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("bistand_vurderinger_id")
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: BistandGrunnlag) {
        val bistandvurderingerId = lagreBistandsvurderinger(nyttGrunnlag.vurderinger)

        connection.execute("INSERT INTO BISTAND_GRUNNLAG (BEHANDLING_ID, BISTAND_VURDERINGER_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, bistandvurderingerId)
            }
        }
    }

    private fun lagreBistandsvurderinger(vurderinger: List<Bistandsvurdering>): Long {
        val bistandvurderingerId = connection.executeReturnKey("""INSERT INTO BISTAND_VURDERINGER DEFAULT VALUES""")

        connection.executeBatch(
            "INSERT INTO BISTAND (BEGRUNNELSE, BEHOV_FOR_AKTIV_BEHANDLING, BEHOV_FOR_ARBEIDSRETTET_TILTAK, BEHOV_FOR_ANNEN_OPPFOELGING, VURDERINGEN_GJELDER_FRA, VURDERT_AV, OVERGANG_BEGRUNNELSE, OVERGANG_TIL_ARBEID, BISTAND_VURDERINGER_ID, VURDERT_I_BEHANDLING) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
                setBoolean(8, vurdering.skalVurdereAapIOvergangTilArbeid)
                setLong(9, bistandvurderingerId)
                setLong(10, vurdering.vurdertIBehandling?.id)
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
            INSERT INTO BISTAND_GRUNNLAG (BEHANDLING_ID, BISTAND_VURDERINGER_ID) 
            SELECT ?, BISTAND_VURDERINGER_ID 
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