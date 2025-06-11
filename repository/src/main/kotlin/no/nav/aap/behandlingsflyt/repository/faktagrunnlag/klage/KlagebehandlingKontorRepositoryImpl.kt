package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagebehandlingKontorGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagebehandlingKontorRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontor
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class KlagebehandlingKontorRepositoryImpl(private val connection: DBConnection) : KlagebehandlingKontorRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object : Factory<KlagebehandlingKontorRepositoryImpl> {
        override fun konstruer(connection: DBConnection): KlagebehandlingKontorRepositoryImpl {
            return KlagebehandlingKontorRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): KlagebehandlingKontorGrunnlag? {
        val query = """
            SELECT * FROM KLAGE_KONTOR_VURDERING
            WHERE id IN (
                SELECT vurdering_id FROM KLAGE_KONTOR_GRUNNLAG
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

    override fun lagre(behandlingId: BehandlingId, klagevurderingKontor: KlagevurderingKontor) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = KlagebehandlingKontorGrunnlag(vurdering = klagevurderingKontor)
        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: KlagebehandlingKontorGrunnlag) {
        val vurderingId = lagreVurdering(nyttGrunnlag.vurdering)
        val query = """
            INSERT INTO KLAGE_KONTOR_GRUNNLAG (BEHANDLING_ID, VURDERING_ID, AKTIV) 
            VALUES (?, ?, TRUE)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    private fun lagreVurdering(vurdering: KlagevurderingKontor): Long {
        val query = """
            INSERT INTO KLAGE_KONTOR_VURDERING
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
            delete from klage_kontor_grunnlag where behandling_id = ?; 
            delete from klage_kontor_vurdering where id = ANY(?::bigint[]);
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, vurderingIds)
            }
        }
        log.info("Slettet $deletedRows rader fra klage_kontor_grunnlag og klage_kontor_vurdering")
    }

    private fun getIdForVurderingerForGrunnlaget(behandlingId: BehandlingId): List<Long> =
        connection.queryList(
            """
                SELECT vurdering_id
                FROM klage_kontor_grunnlag
                WHERE behandling_id = ? AND vurdering_id is not null
                """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row ->
                row.getLong("vurdering_id")
            }
        }

    private fun mapGrunnlag(row: Row): KlagebehandlingKontorGrunnlag {
        return KlagebehandlingKontorGrunnlag(
            vurdering = mapKlagevurderingKontor(row),
        )
    }

    private fun mapKlagevurderingKontor(row: Row): KlagevurderingKontor {
        return KlagevurderingKontor(
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
        connection.execute("UPDATE KLAGE_KONTOR_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

}