package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class MeldepliktRepositoryImpl(private val connection: DBConnection) : MeldepliktRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<MeldepliktRepositoryImpl> {
        override fun konstruer(connection: DBConnection): MeldepliktRepositoryImpl {
            return MeldepliktRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? {
        val query = """
            SELECT f.ID AS MELDEPLIKT_ID, v.HAR_FRITAK, v.FRA_DATO, v.TIL_DATO, v.BEGRUNNELSE, v.OPPRETTET_TID, v.VURDERT_AV, v.VURDERT_I_BEHANDLING 
            FROM MELDEPLIKT_FRITAK_GRUNNLAG g
            INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
            INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()

        return connection.queryList(query) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper(::toMeldepliktInternal)
        }.grupperOgMapTilGrunnlag().firstOrNull()
    }

    override fun hentAlleVurderinger(sakId: SakId, behandlingId: BehandlingId): Set<Fritaksvurdering> {
        val query = """
            SELECT f.ID AS MELDEPLIKT_ID, v.HAR_FRITAK, v.FRA_DATO, v.TIL_DATO, v.BEGRUNNELSE, v.OPPRETTET_TID, v.VURDERT_AV, v.VURDERT_I_BEHANDLING
            FROM MELDEPLIKT_FRITAK_GRUNNLAG g
            INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
            INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
            JOIN BEHANDLING b ON b.ID = g.BEHANDLING_ID
            WHERE g.AKTIV AND b.SAK_ID = ? AND b.opprettet_tid < (SELECT a.opprettet_tid from behandling a where id = ?)
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
                setLong(2, behandlingId.toLong())
            }
            setRowMapper(::toMeldepliktInternal)
        }.map { it.toFritaksvurdering() }.toSet()
    }

    fun toMeldepliktInternal(row: Row): MeldepliktInternal = MeldepliktInternal(
        meldepliktId = row.getLong("MELDEPLIKT_ID"),
        harFritak = row.getBoolean("HAR_FRITAK"),
        fraDato = row.getLocalDate("FRA_DATO"),
        tilDato = row.getLocalDateOrNull("TIL_DATO"),
        begrunnelse = row.getString("BEGRUNNELSE"),
        vurdertAv = row.getString("VURDERT_AV"),
        vurderingOpprettet = row.getLocalDateTime("OPPRETTET_TID"),
        vurdertIBehandling = row.getLongOrNull("VURDERT_I_BEHANDLING")?.let { BehandlingId(it) }
    )


    data class MeldepliktInternal(
        val meldepliktId: Long,
        val harFritak: Boolean,
        val fraDato: LocalDate,
        val tilDato: LocalDate?,
        val begrunnelse: String,
        val vurdertAv: String,
        val vurderingOpprettet: LocalDateTime,
        val vurdertIBehandling: BehandlingId? = null,
    ) {
        fun toFritaksvurdering(): Fritaksvurdering {
            return Fritaksvurdering(
                harFritak = harFritak,
                fraDato = fraDato,
                tilDato = tilDato,
                begrunnelse = begrunnelse,
                vurdertAv = vurdertAv,
                opprettetTid = vurderingOpprettet,
                vurdertIBehandling = vurdertIBehandling
            )
        }
    }

    private fun Iterable<MeldepliktInternal>.grupperOgMapTilGrunnlag(): List<MeldepliktGrunnlag> {
        return groupBy(MeldepliktInternal::meldepliktId) { it.toFritaksvurdering() }
            .map { (_, fritaksvurderinger) ->
                MeldepliktGrunnlag(fritaksvurderinger)
            }
    }

    override fun lagre(behandlingId: BehandlingId, vurderinger: List<Fritaksvurdering>) {
        deaktiverEksisterende(behandlingId)
        val meldepliktId = connection.executeReturnKey("INSERT INTO MELDEPLIKT_FRITAK DEFAULT VALUES")

        connection.execute("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID, MELDEPLIKT_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, meldepliktId)
            }
        }

        connection.executeBatch(
            """
            INSERT INTO MELDEPLIKT_FRITAK_VURDERING 
            (MELDEPLIKT_ID, BEGRUNNELSE, HAR_FRITAK, FRA_DATO, TIL_DATO, VURDERT_AV, OPPRETTET_TID, VURDERT_I_BEHANDLING) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            vurderinger
        ) {
            setParams {
                setLong(1, meldepliktId)
                setString(2, it.begrunnelse)
                setBoolean(3, it.harFritak)
                setLocalDate(4, it.fraDato)
                setLocalDate(5, it.tilDato)
                setString(6, it.vurdertAv)
                setLocalDateTime(7, it.opprettetTid)
                setLong(8, it.vurdertIBehandling?.id)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val meldepliktIds = getMeldepiktIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from meldeplikt_fritak_grunnlag where behandling_id = ?; 
            delete from meldeplikt_fritak_vurdering where meldeplikt_id = ANY(?::bigint[]);
            delete from meldeplikt_fritak where id = ANY(?::bigint[]);
          
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, meldepliktIds)
                setLongArray(3, meldepliktIds)
            }
        }
        log.info("Slettet $deletedRows rader fra meldeplikt_fritak_grunnlag")
    }

    private fun getMeldepiktIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT meldeplikt_id
                    FROM meldeplikt_fritak_grunnlag
                    WHERE behandling_id = ? AND meldeplikt_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("meldeplikt_id")
        }
    }


    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE MELDEPLIKT_FRITAK_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID, MELDEPLIKT_ID) SELECT ?, MELDEPLIKT_ID FROM MELDEPLIKT_FRITAK_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }


    override fun migrerMeldepliktFritak() {
        log.info("Starter migrering av meldeplikt fritak")
        val start = System.currentTimeMillis()

        // Hent alle koblingstabeller
        val kandidater = hentKandidater()
        val kandidaterGruppertPåSak = kandidater.groupBy { it.sakId }

        var migrerteVurderingerCount = 0

        kandidaterGruppertPåSak.forEach { (sakId, kandidaterForSak) ->
            /*if (sakId != SakId(6568)) {
                return@forEach
            }*/
            log.info("Migrerer meldeplikt fritak for sak ${sakId.id} med ${kandidaterForSak.size} kandidater")
            val sorterteKandidater = kandidaterForSak.sortedBy { it.grunnlagOpprettetTid }
            val vurderingerMedVurderingerId =
                hentVurderinger(kandidaterForSak.map { it.meldepliktId }.toSet().toList())

            // Kan skippe grunnlag som peker på vurderinger som allerede er migrert
            val migrerteVurderingerId = mutableSetOf<Long>()

            // Dette dekker en eksisterende vurdering som er lagret som en del av et nytt grunnlag;
            // disse har ikke samme id som den originale.
            // Antar at like vurderinger innenfor samme sak er samme vurdering
            val nyeVerdierForVurdering =
                mutableMapOf<SammenlignbarFritaksvurdering, BehandlingId>()

            sorterteKandidater.forEach { kandidat ->
                if (kandidat.meldepliktId in migrerteVurderingerId) {
                    // Dette er et kopiert grunnlag som allerede er migrert
                    return@forEach
                }
                val vurderingerForGrunnlag =
                    vurderingerMedVurderingerId.filter { it.vurderingerId == kandidat.meldepliktId }

                vurderingerForGrunnlag.forEach { vurderingMedIder ->
                    val vurdering = vurderingMedIder.vurdering
                    val vurderingId = vurderingMedIder.vurderingId
                    val sammenlignbarVurdering = vurdering.tilSammenlignbar()
                    val nyeVerdier = if (nyeVerdierForVurdering.containsKey(sammenlignbarVurdering)) {
                        // Bruk den migrerte versjonen
                        nyeVerdierForVurdering[sammenlignbarVurdering]!!
                    } else {
                        val vurdertIBehandling = kandidat.behandlingId
                        nyeVerdierForVurdering.put(sammenlignbarVurdering, vurdertIBehandling)
                        vurdertIBehandling
                    }

                    connection.execute(
                        """
                        UPDATE MELDEPLIKT_FRITAK_VURDERING
                        SET VURDERT_I_BEHANDLING = ?
                        WHERE ID = ?
                        """.trimIndent()
                    ) {
                        setParams {
                            setLong(1, nyeVerdier.id)
                            setLong(2, vurderingId)
                        }
                    }
                    migrerteVurderingerCount = migrerteVurderingerCount + 1

                    migrerteVurderingerId.add(kandidat.meldepliktId)
                }
            }
        }

        val totalTid = System.currentTimeMillis() - start

        log.info("Fullført migrering av fritak meldeplikt. Migrerte ${kandidater.size} grunnlag og ${migrerteVurderingerCount} vurderinger på $totalTid ms.")
    }


    // Vurdering minus opprettet, tom, vurdertIBehandling
    data class SammenlignbarFritaksvurdering(
        val harFritak: Boolean,
        val fraDato: LocalDate,
        val begrunnelse: String,
        val vurdertAv: String,
    )

    private fun Fritaksvurdering.tilSammenlignbar(): SammenlignbarFritaksvurdering {
        return SammenlignbarFritaksvurdering(
            begrunnelse = this.begrunnelse,
            fraDato = this.fraDato,
            harFritak = this.harFritak,
            vurdertAv = this.vurdertAv,
        )
    }

    private data class VurderingMedVurderingerId(
        val vurderingerId: Long,
        val vurderingId: Long,
        val vurdering: Fritaksvurdering
    )

    private fun hentVurderinger(vurderingerIds: List<Long>): List<VurderingMedVurderingerId> {
        val query = """
            select * from meldeplikt_fritak_vurdering
            where meldeplikt_id = ANY(?::bigint[])
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLongArray(1, vurderingerIds)
            }
            setRowMapper {
                VurderingMedVurderingerId(
                    vurderingerId = it.getLong("meldeplikt_id"),
                    vurderingId = it.getLong("id"),
                    vurdering = toMeldepliktInternal(it).toFritaksvurdering()
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
                     g.meldeplikt_id,
                     g.opprettet_tid as grunnlag_opprettet_tid
            
            from meldeplikt_fritak_grunnlag g 
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
                    meldepliktId = it.getLong("meldeplikt_id"),
                    grunnlagOpprettetTid = it.getLocalDateTime("grunnlag_opprettet_tid"),
                )
            }
        }
    }

    private data class Kandidat(
        val sakId: SakId,
        val grunnlagId: Long,
        val behandlingId: BehandlingId,
        val rettighetsperiode: Periode,
        val meldepliktId: Long, // samme som vurderinger_id i andre vilkår
        val grunnlagOpprettetTid: LocalDateTime,
    )
}

