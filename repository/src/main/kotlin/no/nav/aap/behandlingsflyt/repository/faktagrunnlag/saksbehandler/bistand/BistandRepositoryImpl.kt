package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

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
            id = row.getLong("ID"),
            begrunnelse = row.getString("BEGRUNNELSE"),
            erBehovForAktivBehandling = row.getBoolean("BEHOV_FOR_AKTIV_BEHANDLING"),
            erBehovForArbeidsrettetTiltak = row.getBoolean("BEHOV_FOR_ARBEIDSRETTET_TILTAK"),
            erBehovForAnnenOppfølging = row.getBooleanOrNull("BEHOV_FOR_ANNEN_OPPFOELGING"),
            vurderingenGjelderFra = row.getLocalDateOrNull("VURDERINGEN_GJELDER_FRA"),
            skalVurdereAapIOvergangTilUføre = row.getBooleanOrNull("OVERGANG_TIL_UFOERE"),
            skalVurdereAapIOvergangTilArbeid = row.getBooleanOrNull("OVERGANG_TIL_ARBEID"),
            overgangBegrunnelse = row.getStringOrNull("OVERGANG_BEGRUNNELSE"),
            vurdertAv = row.getString("VURDERT_AV"),
            opprettet = row.getInstant("OPPRETTET_TID"),
            vurdertIBehandling = row.getLongOrNull("VURDERT_I_BEHANDLING")?.let(::BehandlingId),
        )
    }

    override fun hentHistoriskeBistandsvurderinger(sakId: SakId, behandlingId: BehandlingId): List<BistandVurdering> {
        val query = """WITH vurderinger_historikk AS (
            SELECT DISTINCT ON (
                b.begrunnelse,
                b.behov_for_aktiv_behandling,
                b.behov_for_arbeidsrettet_tiltak,
                b.behov_for_annen_oppfoelging,
                b.vurderingen_gjelder_fra,
                b.vurdert_av,
                b.overgang_til_ufoere,
                b.overgang_til_arbeid,
                b.overgang_begrunnelse
                )
                b.*
            FROM bistand_grunnlag g
                     JOIN bistand_vurderinger v ON g.bistand_vurderinger_id = v.id
                     JOIN bistand b ON b.bistand_vurderinger_id = v.id
                     JOIN behandling beh ON g.behandling_id = beh.id
                     LEFT JOIN avbryt_revurdering_grunnlag ar ON ar.behandling_id = beh.id
            WHERE g.aktiv
              AND beh.sak_id = ?
              AND beh.opprettet_tid < (
                SELECT opprettet_tid
                FROM behandling
                WHERE id = ?
            )
                AND ar.behandling_id IS NULL
        )
        SELECT * FROM vurderinger_historikk;
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
            vurderinger = bistandsvurderinger
        )

        val eksisterendeVurderinger =
            eksisterendeBistandGrunnlag?.vurderinger?.let { it.map { it.copy(opprettet = null, vurdertIBehandling = null) } }.orEmpty().toSet()
        val nyeVurderinger = bistandsvurderinger.map { it.copy(opprettet = null, vurdertIBehandling = null) }.toSet()

        if (eksisterendeVurderinger != nyeVurderinger) {
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

    private fun lagreBistandsvurderinger(vurderinger: List<BistandVurdering>): Long {
        val bistandvurderingerId = connection.executeReturnKey("""INSERT INTO BISTAND_VURDERINGER DEFAULT VALUES""")

        connection.executeBatch(
            "INSERT INTO BISTAND (BEGRUNNELSE, BEHOV_FOR_AKTIV_BEHANDLING, BEHOV_FOR_ARBEIDSRETTET_TILTAK, BEHOV_FOR_ANNEN_OPPFOELGING, VURDERINGEN_GJELDER_FRA, VURDERT_AV, OVERGANG_BEGRUNNELSE, OVERGANG_TIL_UFOERE, OVERGANG_TIL_ARBEID, BISTAND_VURDERINGER_ID, VURDERT_I_BEHANDLING) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
                setLong(11, vurdering.vurdertIBehandling?.id)
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


    override fun migrerBistandsvurderinger() {
        log.info("Starter migrering av bistandsvurderinger")
        val start = System.currentTimeMillis()

        // Hent alle koblingstabeller
        val kandidater = hentKandidater()
        val kandidaterGruppertPåSak = kandidater.groupBy { it.sakId }

        var migrerteVurderingerCount = 0

        kandidaterGruppertPåSak.forEach { (sakId, kandidaterForSak) ->
            log.info("Migrerer bistandsvurderinger for sak ${sakId.id} med ${kandidaterForSak.size} kandidater")
            val sorterteKandidater = kandidaterForSak.sortedBy { it.grunnlagOpprettetTid }
            val vurderingerMedVurderingerId =
                hentVurderinger(kandidaterForSak.map { it.vurderingerId }.toSet().toList())

            // Kan skippe grunnlag som peker på vurderinger som allerede er migrert
            val migrerteVurderingerId = mutableSetOf<Long>()

            // Dette dekker en eksisterende vurdering som er lagret som en del av et nytt grunnlag; 
            // disse har ikke samme id som den originale.
            // Antar at like vurderinger innenfor samme sak er samme vurdering
            val nyeVerdierForVurdering = mutableMapOf<SammenlignbarBistandVurdering, Pair<BehandlingId, LocalDate>>()

            sorterteKandidater.forEach { kandidat ->
                if (kandidat.vurderingerId in migrerteVurderingerId) {
                    // Dette er et kopiert grunnlag som allerede er migrert
                    return@forEach
                }
                val vurderingerForGrunnlag =
                    vurderingerMedVurderingerId.filter { it.vurderingerId == kandidat.vurderingerId }.map{it.bistandsvurdering}

                vurderingerForGrunnlag.forEach { vurdering ->
                    val sammenlignbarVurdering = vurdering.tilSammenlignbar()
                    val nyeVerdier = if (nyeVerdierForVurdering.containsKey(sammenlignbarVurdering)) {
                        // Bruk den migrerte versjonen
                        nyeVerdierForVurdering[sammenlignbarVurdering]!!
                    } else {
                        val nyeVerdier = Pair(kandidat.behandlingId, vurdering.vurderingenGjelderFra ?: kandidat.rettighetsperiode.fom)
                        nyeVerdierForVurdering.put(sammenlignbarVurdering, nyeVerdier)
                        nyeVerdier
                    }

                    // Oppdater
                    connection.execute(
                        """
                        UPDATE BISTAND
                        SET VURDERT_I_BEHANDLING = ?, VURDERINGEN_GJELDER_FRA = ?
                        WHERE ID = ?
                        """.trimIndent()
                    ) {
                        setParams {
                            setLong(1, nyeVerdier.first.id)
                            setLocalDate(2, nyeVerdier.second)
                            setLong(3, vurdering.id!!)
                        }
                    }
                    migrerteVurderingerCount = migrerteVurderingerCount + 1

                    migrerteVurderingerId.add(kandidat.vurderingerId)
                }
            }
        }

        val totalTid = System.currentTimeMillis() - start

        log.info("Fullført migrering av manuelle vurderinger for bistand. Migrerte ${kandidater.size} grunnlag og ${migrerteVurderingerCount} vurderinger på $totalTid ms.")
    }


    private data class Kandidat(
        val sakId: SakId,
        val grunnlagId: Long,
        val behandlingId: BehandlingId,
        val rettighetsperiode: Periode,
        val vurderingerId: Long,
        val grunnlagOpprettetTid: LocalDateTime,
    )

    // Vurdering minus opprettet, id, vurdertIBehandling
    data class SammenlignbarBistandVurdering(
        val begrunnelse: String,
        val erBehovForAktivBehandling: Boolean,
        val erBehovForArbeidsrettetTiltak: Boolean,
        val erBehovForAnnenOppfølging: Boolean?,
        val overgangBegrunnelse: String?,
        val skalVurdereAapIOvergangTilArbeid: Boolean?,
        @Deprecated("""Det er i utgangspunktet Kelvin som avgjør om det mangler en vurdering av overgang til uføre når det kan være relevant.""")
        val skalVurdereAapIOvergangTilUføre: Boolean?,
        val vurdertAv: String,
        val vurderingenGjelderFra: LocalDate?,
    )
    private fun BistandVurdering.tilSammenlignbar(): SammenlignbarBistandVurdering {
        return SammenlignbarBistandVurdering(
            begrunnelse = this.begrunnelse,
            erBehovForAktivBehandling = this.erBehovForAktivBehandling,
            erBehovForArbeidsrettetTiltak = this.erBehovForArbeidsrettetTiltak,
            erBehovForAnnenOppfølging = this.erBehovForAnnenOppfølging,
            overgangBegrunnelse = this.overgangBegrunnelse,
            skalVurdereAapIOvergangTilArbeid = this.skalVurdereAapIOvergangTilArbeid,
            skalVurdereAapIOvergangTilUføre = this.skalVurdereAapIOvergangTilUføre, 
            vurdertAv = this.vurdertAv,
            vurderingenGjelderFra = this.vurderingenGjelderFra,
        )
    }

    private data class VurderingMedVurderingerId(
        val vurderingerId: Long,
        val bistandsvurdering: BistandVurdering
    )

    private fun hentVurderinger(vurderingerIds: List<Long>): List<VurderingMedVurderingerId> {
        val query = """
            select  * from bistand
            where bistand_vurderinger_id = ANY(?::bigint[])
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLongArray(1, vurderingerIds)
            }
            setRowMapper {
                VurderingMedVurderingerId(
                    vurderingerId = it.getLong("bistand_vurderinger_id"),
                    bistandsvurdering = bistandvurderingRowMapper(it)
                )
            }
        }
    }

    private fun hentKandidater(): List<Kandidat> {
        val kandidaterQuery = """
            select g.id as grunnlag_id,
                     b.id as behandling_id,
                     s.id as sak_id,
                     s.rettighetsperiode,
                     g.bistand_vurderinger_id,
                     g.opprettet_tid as grunnlag_opprettet_tid
            
            from bistand_grunnlag g 
            inner join behandling b on g.behandling_id = b.id
            inner join sak s on b.sak_id = s.id
        """.trimIndent()

        return connection.queryList(kandidaterQuery) {
            setRowMapper {
                Kandidat(
                    sakId = SakId(it.getLong("sak_id")),
                    grunnlagId = it.getLong("grunnlag_id"),
                    behandlingId = BehandlingId(it.getLong("behandling_id")),
                    rettighetsperiode = it.getPeriode("rettighetsperiode"), // Denne er teknisk sett feil, men kanskje godt nok. Hvis ikke: join på rettighetsperiode_grunnlag
                    vurderingerId = it.getLong("bistand_vurderinger_id"),
                    grunnlagOpprettetTid = it.getLocalDateTime("grunnlag_opprettet_tid"),
                )
            }
        }
    }

}