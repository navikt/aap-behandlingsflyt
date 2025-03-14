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
            SELECT g.ID, b.vurderingen_gjelder_fra, b.BEGRUNNELSE, b.behov_for_aktiv_behandling, b.behov_for_arbeidsrettet_tiltak, b.behov_for_annen_oppfoelging
            FROM BISTAND_GRUNNLAG g
            INNER JOIN BISTAND b ON g.BISTAND_ID = b.ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                BistandGrunnlag(
                    id = row.getLong("ID"),
                    behandlingId = behandlingId,
                    vurdering = bistandvurderingRowMapper(row)
                )
            }
        }
    }

    private fun bistandvurderingRowMapper(row: Row): BistandVurdering {
        return BistandVurdering(
            begrunnelse = row.getString("BEGRUNNELSE"),
            erBehovForAktivBehandling = row.getBoolean("BEHOV_FOR_AKTIV_BEHANDLING"),
            erBehovForArbeidsrettetTiltak = row.getBoolean("BEHOV_FOR_ARBEIDSRETTET_TILTAK"),
            erBehovForAnnenOppfølging = row.getBooleanOrNull("BEHOV_FOR_ANNEN_OPPFOELGING"),
            vurderingenGjelderFra = row.getLocalDateOrNull("VURDERINGEN_GJELDER_FRA")
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

    override fun lagre(behandlingId: BehandlingId, bistandVurdering: BistandVurdering) {
        val eksisterendeBistandGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeBistandGrunnlag?.vurdering == bistandVurdering) return

        if (eksisterendeBistandGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val bistandId =
            connection.executeReturnKey("INSERT INTO BISTAND (BEGRUNNELSE, BEHOV_FOR_AKTIV_BEHANDLING, BEHOV_FOR_ARBEIDSRETTET_TILTAK, BEHOV_FOR_ANNEN_OPPFOELGING, VURDERINGEN_GJELDER_FRA) VALUES (?, ?, ?, ?, ?)") {
                setParams {
                    setString(1, bistandVurdering.begrunnelse)
                    setBoolean(2, bistandVurdering.erBehovForAktivBehandling)
                    setBoolean(3, bistandVurdering.erBehovForArbeidsrettetTiltak)
                    setBoolean(4, bistandVurdering.erBehovForAnnenOppfølging)
                    setLocalDate(5, bistandVurdering.vurderingenGjelderFra)
                }
            }

        connection.execute("INSERT INTO BISTAND_GRUNNLAG (BEHANDLING_ID, BISTAND_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, bistandId)
            }
        }
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
        connection.execute("INSERT INTO BISTAND_GRUNNLAG (BEHANDLING_ID, BISTAND_ID) SELECT ?, BISTAND_ID FROM BISTAND_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}