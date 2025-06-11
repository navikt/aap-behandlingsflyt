package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNay
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class KlagebehandlingNayRepositoryImpl(private val connection: DBConnection) : KlagebehandlingNayRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object : Factory<KlagebehandlingNayRepositoryImpl> {
        override fun konstruer(connection: DBConnection): KlagebehandlingNayRepositoryImpl {
            return KlagebehandlingNayRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): KlagebehandlingNayGrunnlag? {
        val query = """
            SELECT * FROM KLAGE_NAY_VURDERING
            WHERE id IN (
                SELECT vurdering_id FROM KLAGE_NAY_GRUNNLAG
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

    override fun lagre(behandlingId: BehandlingId, klagevurderingNay: KlagevurderingNay) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = KlagebehandlingNayGrunnlag(vurdering = klagevurderingNay)
        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: KlagebehandlingNayGrunnlag) {
        val vurderingId = lagreVurdering(nyttGrunnlag.vurdering)
        val query = """
            INSERT INTO KLAGE_NAY_GRUNNLAG (BEHANDLING_ID, VURDERING_ID, AKTIV) 
            VALUES (?, ?, TRUE)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    private fun lagreVurdering(vurdering: KlagevurderingNay): Long {
        val query = """
            INSERT INTO KLAGE_NAY_VURDERING
            (BEGRUNNELSE, NOTAT, INNSTILLING, VILKAAR_SOM_SKAL_OMGJOERES, VILKAAR_SOM_SKAL_OPPRETTHOLDES, VURDERT_AV)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setString(2, vurdering.notat)
                setEnumName(3, vurdering.innstilling)
                setArray(4, vurdering.vilkårSomOmgjøres.map { it.hjemmel })
                setArray(5, vurdering.vilkårSomOpprettholdes.map { it.hjemmel })
                setString(6, vurdering.vurdertAv)
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

        val deletedRows = connection.executeReturnUpdated("""
            delete from klage_nay_grunnlag where behandling_id = ?; 
            delete from klage_nay_vurdering where id = ANY(?::bigint[]);
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, vurderingIds)
            }
        }
        log.info("Slettet $deletedRows rader fra klage_nay_grunnlag og klage_nay_vurdering")
    }

    private fun getIdForVurderingerForGrunnlaget(behandlingId: BehandlingId): List<Long> =
        connection.queryList(
            """
                SELECT vurdering_id
                FROM klage_nay_grunnlag
                WHERE behandling_id = ? AND vurdering_id is not null
                """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row ->
                row.getLong("vurdering_id")
            }
        }

    private fun mapGrunnlag(row: Row): KlagebehandlingNayGrunnlag {
        return KlagebehandlingNayGrunnlag(
            vurdering = mapKlagevurderingNay(row),
        )
    }

    private fun mapKlagevurderingNay(row: Row): KlagevurderingNay {
        return KlagevurderingNay(
            begrunnelse = row.getString("BEGRUNNELSE"),
            notat = row.getStringOrNull("NOTAT"),
            innstilling = row.getEnum("INNSTILLING"),
            vilkårSomOmgjøres = row.getArray("vilkaar_som_skal_omgjoeres", String::class)
                .mapNotNull { Hjemmel.fraHjemmel(it) },
            vilkårSomOpprettholdes = row.getArray("vilkaar_som_skal_opprettholdes", String::class)
                .mapNotNull { Hjemmel.fraHjemmel(it) },
            vurdertAv = row.getString("VURDERT_AV"),
            opprettet = row.getInstant("opprettet_tid")
        )
    }


    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE KLAGE_NAY_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

}