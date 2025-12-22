package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

class ArbeidsevneRepositoryImpl(private val connection: DBConnection) : ArbeidsevneRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<ArbeidsevneRepositoryImpl> {
        override fun konstruer(connection: DBConnection): ArbeidsevneRepositoryImpl {
            return ArbeidsevneRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsevneGrunnlag? {
        return connection.queryList(
            """
            SELECT a.ID AS ARBEIDSEVNE_ID, v.BEGRUNNELSE, v.FRA_DATO, v.TIL_DATO, v.ANDEL_ARBEIDSEVNE, v.VURDERT_I_BEHANDLING, v.OPPRETTET_TID, v.VURDERT_AV
            FROM ARBEIDSEVNE_GRUNNLAG g
            INNER JOIN ARBEIDSEVNE a ON g.ARBEIDSEVNE_ID = a.ID
            INNER JOIN ARBEIDSEVNE_VURDERING v ON a.ID = v.ARBEIDSEVNE_ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper(::toArbeidsevneInternal)
        }.toGrunnlag()
    }

    override fun hentAlleVurderinger(sakId: SakId, behandlingId: BehandlingId): Set<ArbeidsevneVurdering> {
        val query = """
            SELECT a.ID AS ARBEIDSEVNE_ID, v.BEGRUNNELSE, v.FRA_DATO, v.TIL_DATO, v.ANDEL_ARBEIDSEVNE, v.VURDERT_I_BEHANDLING, v.OPPRETTET_TID,  v.VURDERT_AV
            FROM ARBEIDSEVNE_GRUNNLAG g
            INNER JOIN ARBEIDSEVNE a ON g.ARBEIDSEVNE_ID = a.ID
            INNER JOIN ARBEIDSEVNE_VURDERING v ON a.ID = v.ARBEIDSEVNE_ID
            JOIN BEHANDLING b ON b.ID = g.BEHANDLING_ID
            WHERE g.AKTIV AND b.SAK_ID = ? AND b.opprettet_tid < (SELECT bh.opprettet_tid from behandling bh where id = ?)
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
                setLong(2, behandlingId.toLong())
            }
            setRowMapper(::toArbeidsevneInternal)
        }.map { it.toArbeidsevnevurdering() }.toSet()
    }

    fun toArbeidsevneInternal(row: Row): ArbeidsevneInternal = ArbeidsevneInternal(
        arbeidsevneId = row.getLong("ARBEIDSEVNE_ID"),
        begrunnelse = row.getString("BEGRUNNELSE"),
        fraDato = row.getLocalDate("FRA_DATO"), tilDato = row.getLocalDateOrNull("TIL_DATO"),
        arbeidsevne = Prosent(row.getInt("ANDEL_ARBEIDSEVNE")),
        vurdertIBehandling = row.getLongOrNull("VURDERT_I_BEHANDLING")?.let { BehandlingId(it) },
        opprettetTid = row.getLocalDateTime("OPPRETTET_TID"),
        vurdertAv = row.getString("VURDERT_AV")
    )

    data class ArbeidsevneInternal(
        val arbeidsevneId: Long,
        val begrunnelse: String,
        val fraDato: LocalDate,
        val tilDato: LocalDate?,
        val arbeidsevne: Prosent,
        val vurdertIBehandling: BehandlingId?,
        val opprettetTid: LocalDateTime,
        val vurdertAv: String
    ) {
        fun toArbeidsevnevurdering(): ArbeidsevneVurdering {
            return ArbeidsevneVurdering(begrunnelse, arbeidsevne, fraDato, tilDato, vurdertIBehandling, opprettetTid, vurdertAv)
        }
    }

    private fun List<ArbeidsevneInternal>.toGrunnlag(): ArbeidsevneGrunnlag? {
        return groupBy(ArbeidsevneInternal::arbeidsevneId, ArbeidsevneInternal::toArbeidsevnevurdering)
            .map { (_, arbeidsevneVurderinger) -> ArbeidsevneGrunnlag(arbeidsevneVurderinger) }
            .takeIf { it.isNotEmpty() }
            ?.single()
    }

    override fun lagre(behandlingId: BehandlingId, vurderinger: List<ArbeidsevneVurdering>) {
        deaktiverEksisterende(behandlingId)

        val arbeidsevneId = connection.executeReturnKey("INSERT INTO ARBEIDSEVNE DEFAULT VALUES")

        connection.execute("INSERT INTO ARBEIDSEVNE_GRUNNLAG (BEHANDLING_ID, ARBEIDSEVNE_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, arbeidsevneId)
            }
        }

        vurderinger.lagre(arbeidsevneId)
    }

    private fun List<ArbeidsevneVurdering>.lagre(arbeidsevneId: Long) {
        connection.executeBatch(
            """
            INSERT INTO ARBEIDSEVNE_VURDERING 
            (ARBEIDSEVNE_ID, FRA_DATO, TIL_DATO, BEGRUNNELSE, ANDEL_ARBEIDSEVNE, VURDERT_I_BEHANDLING, OPPRETTET_TID, VURDERT_AV) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            this
        ) {
            setParams {
                setLong(1, arbeidsevneId)
                setLocalDate(2, it.fraDato)
                setLocalDate(3, it.tilDato)
                setString(4, it.begrunnelse)
                setInt(5, it.arbeidsevne.prosentverdi())
                setLong(6, it.vurdertIBehandling?.id)
                setLocalDateTime(7, it.opprettetTid)
                setString(8, it.vurdertAv)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE ARBEIDSEVNE_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute("INSERT INTO ARBEIDSEVNE_GRUNNLAG (BEHANDLING_ID, ARBEIDSEVNE_ID) SELECT ?, ARBEIDSEVNE_ID FROM ARBEIDSEVNE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val arbeidsevneIds = getArbeidsevneIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from arbeidsevne_grunnlag where behandling_id = ?; 
            delete from arbeidsevne_vurdering where arbeidsevne_id = ANY(?::bigint[]);
            delete from arbeidsevne where id = ANY(?::bigint[]);
           
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, arbeidsevneIds)
                setLongArray(3, arbeidsevneIds)

            }
        }
        log.info("Slettet $deletedRows rader fra arbeidsevne_grunnlag")
    }

    private fun getArbeidsevneIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT arbeidsevne_id
                    FROM arbeidsevne_grunnlag
                    WHERE behandling_id = ? AND arbeidsevne_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("arbeidsevne_id")
        }
    }


    override fun migrerArbeidsevne() {
        log.info("Starter migrering av arbeidsevne")
        // Hent alle koblingstabeller
        val kandidater = hentKandidater()
        val kandidaterGruppertPåSak = kandidater.groupBy { it.sakId }

        var migrerteVurderingerCount = 0
        val totalTid = measureTimeMillis {
            kandidaterGruppertPåSak.forEach { (sakId, kandidaterForSak) ->
                log.info("Migrerer arbeidsevne for sak ${sakId.id} med ${kandidaterForSak.size} kandidater")
                val sorterteKandidater = kandidaterForSak.sortedBy { it.grunnlagOpprettetTid }
                val vurderingerMedVurderingerId =
                    hentVurderinger(kandidaterForSak.map { it.arbeidsevneId }.toSet().toList())

                // Kan skippe grunnlag som peker på vurderinger som allerede er migrert
                val migrerteVurderingerId = mutableSetOf<Long>()

                // Dette dekker en eksisterende vurdering som er lagret som en del av et nytt grunnlag;
                // disse har ikke samme id som den originale.
                // Antar at like vurderinger innenfor samme sak er samme vurdering
                val nyeVerdierForVurdering =
                    mutableMapOf<SammenlignbarArbeidsevnevurdering, BehandlingId>()

                sorterteKandidater.filterNot { it.arbeidsevneId in migrerteVurderingerId }.forEach { kandidat ->
                    val vurderingerForGrunnlag =
                        vurderingerMedVurderingerId.filter { it.vurderingerId == kandidat.arbeidsevneId }

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
                        UPDATE ARBEIDSEVNE_VURDERING
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

                        migrerteVurderingerId.add(kandidat.arbeidsevneId)
                    }
                }
            }
        }
        log.info("Fullført migrering av arbeidsevne. Migrerte ${kandidater.size} grunnlag og ${migrerteVurderingerCount} vurderinger på $totalTid ms.")
    }



    // Vurdering minus opprettet, tom, vurdertIBehandling
    data class SammenlignbarArbeidsevnevurdering(
        val arbeidsevne: Int,
        val fraDato: LocalDate,
        val begrunnelse: String,
        val vurdertAv: String,
    )

    private fun ArbeidsevneVurdering.tilSammenlignbar(): SammenlignbarArbeidsevnevurdering {
        return SammenlignbarArbeidsevnevurdering(
            begrunnelse = this.begrunnelse,
            fraDato = this.fraDato,
            arbeidsevne = this.arbeidsevne.prosentverdi(),
            vurdertAv = this.vurdertAv,
        )
    }

    private data class VurderingMedVurderingerId(
        val vurderingerId: Long,
        val vurderingId: Long,
        val vurdering: ArbeidsevneVurdering
    )

    private fun hentVurderinger(vurderingerIds: List<Long>): List<VurderingMedVurderingerId> {
        val query = """
            select * from arbeidsevne_vurdering
            where arbeidsevne_id = ANY(?::bigint[])
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLongArray(1, vurderingerIds)
            }
            setRowMapper {
                VurderingMedVurderingerId(
                    vurderingerId = it.getLong("arbeidsevne_id"),
                    vurderingId = it.getLong("id"),
                    vurdering = toArbeidsevneInternal(it).toArbeidsevnevurdering()
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
                     g.arbeidsevne_id,
                     g.opprettet_tid as grunnlag_opprettet_tid
            
            from arbeidsevne_grunnlag g 
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
                    arbeidsevneId = it.getLong("arbeidsevne_id"),
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
        val arbeidsevneId: Long, // samme som vurderinger_id i andre vilkår
        val grunnlagOpprettetTid: LocalDateTime,
    )
}
