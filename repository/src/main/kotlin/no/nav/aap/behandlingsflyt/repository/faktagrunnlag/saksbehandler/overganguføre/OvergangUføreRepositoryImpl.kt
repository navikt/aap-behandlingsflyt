package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.overganguføre

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class OvergangUføreRepositoryImpl(private val connection: DBConnection) : OvergangUføreRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<OvergangUføreRepositoryImpl> {
        override fun konstruer(connection: DBConnection): OvergangUføreRepositoryImpl {
            return OvergangUføreRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): OvergangUføreGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT ID, VURDERINGER_ID
            FROM OVERGANG_UFORE_GRUNNLAG
            WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                OvergangUføreGrunnlag(
                    id = row.getLong("ID"),
                    vurderinger = mapOvergangUforevurderinger(row.getLongOrNull("VURDERINGER_ID"))
                )
            }
        }
    }

    private fun mapOvergangUforevurderinger(overgangUforevurderingerId: Long?): List<OvergangUføreVurdering> {
        return connection.queryList(
            """
                SELECT * FROM OVERGANG_UFORE_VURDERING WHERE VURDERINGER_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, overgangUforevurderingerId)
            }
            setRowMapper(::overgangUforevurderingRowMapper)
        }
    }

    private fun overgangUforevurderingRowMapper(row: Row): OvergangUføreVurdering {
        return OvergangUføreVurdering(
            begrunnelse = row.getString("BEGRUNNELSE"),
            brukerHarSøktOmUføretrygd = row.getBoolean("BRUKER_SOKT_UFORETRYGD"),
            brukerHarFåttVedtakOmUføretrygd = row.getStringOrNull("BRUKER_VEDTAK_UFORETRYGD"),
            brukerRettPåAAP = row.getBooleanOrNull("BRUKER_RETT_PAA_AAP"),
            vurdertIBehandling = row.getLongOrNull("VURDERT_I_BEHANDLING")?.let(::BehandlingId),
            fom = row.getLocalDateOrNull("VIRKNINGSDATO"), // Virkningsdato er 'vurderingen gjelder fra'
            vurdertAv = row.getString("VURDERT_AV"),
            opprettet = row.getInstant("OPPRETTET_TID"),
            tom = row.getLocalDateOrNull("TOM")
        )
    }

    override fun hentHistoriskeOvergangUforeVurderinger(
        sakId: SakId,
        behandlingId: BehandlingId
    ): List<OvergangUføreVurdering> {
        val query = """
            SELECT DISTINCT overgang_ufore_vurdering.*
            FROM overgang_ufore_grunnlag grunnlag
            INNER JOIN overgang_ufore_vurderinger ON grunnlag.vurderinger_id = overgang_ufore_vurderinger.id
            INNER JOIN overgang_ufore_vurdering ON overgang_ufore_vurdering.vurderinger_id = overgang_ufore_vurderinger.id
            INNER JOIN behandling ON grunnlag.behandling_id = behandling.id
            LEFT JOIN avbryt_revurdering_grunnlag ar ON ar.behandling_id = behandling.id
            WHERE grunnlag.aktiv AND behandling.sak_id = ?
                AND behandling.opprettet_tid < (select a.opprettet_tid from behandling a where id = ?)
                AND ar.behandling_id IS NULL
            ORDER BY overgang_ufore_vurdering.opprettet_tid
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.id)
                setLong(2, behandlingId.id)
            }
            setRowMapper(::overgangUforevurderingRowMapper)
        }
    }

    override fun lagre(behandlingId: BehandlingId, overgangUføreVurderinger: List<OvergangUføreVurdering>) {
        val overgangUforeGrunnlag = hentHvisEksisterer(behandlingId)

        val nyttGrunnlag = OvergangUføreGrunnlag(
            id = null,
            vurderinger = overgangUføreVurderinger
        )

        if (overgangUforeGrunnlag != nyttGrunnlag) {
            overgangUforeGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val overgangUforeVurderingerIds = getOvergangUforeVurderingerIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from overgang_ufore_grunnlag where behandling_id = ?; 
            delete from overgang_ufore_vurdering where vurderinger_id = ANY(?::bigint[]);
            delete from overgang_ufore_vurderinger where id = ANY(?::bigint[]);
           
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, overgangUforeVurderingerIds)
                setLongArray(3, overgangUforeVurderingerIds)
            }
        }
        log.info("Slettet $deletedRows rader fra overgang_ufore_grunnlag")
    }

    private fun getOvergangUforeVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT vurderinger_id
                    FROM overgang_ufore_grunnlag
                    WHERE behandling_id = ? AND vurderinger_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurderinger_id")
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: OvergangUføreGrunnlag) {
        val overgangUforevurderingerId = lagreOvergangUforevurderinger(nyttGrunnlag.vurderinger)

        connection.execute("INSERT INTO OVERGANG_UFORE_GRUNNLAG (BEHANDLING_ID, VURDERINGER_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, overgangUforevurderingerId)
            }
        }
    }

    private fun lagreOvergangUforevurderinger(vurderinger: List<OvergangUføreVurdering>): Long {
        val overganguforevurderingerId =
            connection.executeReturnKey("""INSERT INTO OVERGANG_UFORE_VURDERINGER DEFAULT VALUES""")

        connection.executeBatch(
            "INSERT INTO OVERGANG_UFORE_VURDERING (BEGRUNNELSE, BRUKER_SOKT_UFORETRYGD, BRUKER_VEDTAK_UFORETRYGD, BRUKER_RETT_PAA_AAP, VIRKNINGSDATO, VURDERT_AV, VURDERINGER_ID, VURDERT_I_BEHANDLING, TOM) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            vurderinger
        ) {
            setParams { vurdering ->
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.brukerHarSøktOmUføretrygd)
                setString(3, vurdering.brukerHarFåttVedtakOmUføretrygd)
                setBoolean(4, vurdering.brukerRettPåAAP)
                setLocalDate(5, vurdering.fom)
                setString(6, vurdering.vurdertAv)
                setLong(7, overganguforevurderingerId)
                setLong(8, vurdering.vurdertIBehandling?.toLong())
                setLocalDate(9, vurdering.tom)
            }
        }

        return overganguforevurderingerId
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE OVERGANG_UFORE_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
            INSERT INTO OVERGANG_UFORE_GRUNNLAG (BEHANDLING_ID, VURDERINGER_ID) 
            SELECT ?, VURDERINGER_ID 
            FROM OVERGANG_UFORE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun migrerOvergangUføre() {
        log.info("Starter migrering av overgang uføre")
        val start = System.currentTimeMillis()

        // Hent alle koblingstabeller
        val kandidater = hentKandidater()
        val kandidaterGruppertPåSak = kandidater.groupBy { it.sakId }

        var migrerteVurderingerCount = 0

        kandidaterGruppertPåSak.forEach { (sakId, kandidaterForSak) ->
            log.info("Migrerer overgang uføre for sak ${sakId.id} med ${kandidaterForSak.size} kandidater")
            val sorterteKandidater = kandidaterForSak.sortedBy { it.grunnlagOpprettetTid }
            val vurderingerMedVurderingerId =
                hentVurderinger(kandidaterForSak.map { it.vurderingerId }.toSet().toList())

            // Kan skippe grunnlag som peker på vurderinger som allerede er migrert
            val migrerteVurderingerId = mutableSetOf<Long>()

            // Dette dekker en eksisterende vurdering som er lagret som en del av et nytt grunnlag; 
            // disse har ikke samme id som den originale.
            // Antar at like vurderinger innenfor samme sak er samme vurdering
            val nyeVerdierForVurdering =
                mutableMapOf<SammenlignbarOvergangUføreVurdering, Pair<BehandlingId, LocalDate>>()

            sorterteKandidater.forEach { kandidat ->
                if (kandidat.vurderingerId in migrerteVurderingerId) {
                    // Dette er et kopiert grunnlag som allerede er migrert
                    return@forEach
                }
                val vurderingerForGrunnlag =
                    vurderingerMedVurderingerId.filter { it.vurderingerId == kandidat.vurderingerId }

                vurderingerForGrunnlag.forEach { vurderingMedIder ->
                    val vurdering = vurderingMedIder.vurdering
                    val vurderingId = vurderingMedIder.vurderingId
                    val sammenlignbarVurdering = vurdering.tilSammenlignbar()
                    val nyeVerdier = if (nyeVerdierForVurdering.containsKey(sammenlignbarVurdering)) {
                        // Bruk den migrerte versjonen
                        nyeVerdierForVurdering[sammenlignbarVurdering]!!
                    } else {
                        val nyeVerdier = Pair(
                            kandidat.behandlingId,
                            vurdering.fom ?: kandidat.rettighetsperiode.fom
                        )
                        nyeVerdierForVurdering.put(sammenlignbarVurdering, nyeVerdier)
                        nyeVerdier
                    }

                    connection.execute(
                        """
                        UPDATE OVERGANG_UFORE_VURDERING
                        SET VURDERT_I_BEHANDLING = ?, virkningsdato = ?
                        WHERE ID = ?
                        """.trimIndent()
                    ) {
                        setParams {
                            setLong(1, nyeVerdier.first.id)
                            setLocalDate(2, nyeVerdier.second)
                            setLong(3, vurderingId)
                        }
                    }
                    migrerteVurderingerCount = migrerteVurderingerCount + 1

                    migrerteVurderingerId.add(kandidat.vurderingerId)
                }
            }
        }

        val totalTid = System.currentTimeMillis() - start

        log.info("Fullført migrering av overgang uføre. Migrerte ${kandidater.size} grunnlag og ${migrerteVurderingerCount} vurderinger på $totalTid ms.")
    }


    // Vurdering minus opprettet, tom, vurdertIBehandling
    data class SammenlignbarOvergangUføreVurdering(
        val begrunnelse: String,
        val brukerHarSøktOmUføretrygd: Boolean,
        val brukerHarFåttVedtakOmUføretrygd: String?,
        val brukerRettPåAAP: Boolean?,
        val fom: LocalDate?,
        val vurdertAv: String,
    )

    private fun OvergangUføreVurdering.tilSammenlignbar(): SammenlignbarOvergangUføreVurdering {
        return SammenlignbarOvergangUføreVurdering(
            begrunnelse = this.begrunnelse,
            brukerHarSøktOmUføretrygd = this.brukerHarSøktOmUføretrygd,
            brukerHarFåttVedtakOmUføretrygd = this.brukerHarFåttVedtakOmUføretrygd,
            brukerRettPåAAP = this.brukerRettPåAAP,
            fom = this.fom,
            vurdertAv = this.vurdertAv,
        )
    }

    private data class VurderingMedVurderingerId(
        val vurderingerId: Long,
        val vurderingId: Long,
        val vurdering: OvergangUføreVurdering
    )

    private fun hentVurderinger(vurderingerIds: List<Long>): List<VurderingMedVurderingerId> {
        val query = """
            select  * from overgang_ufore_vurdering
            where vurderinger_id = ANY(?::bigint[])
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLongArray(1, vurderingerIds)
            }
            setRowMapper {
                VurderingMedVurderingerId(
                    vurderingerId = it.getLong("vurderinger_id"),
                    vurderingId = it.getLong("id"),
                    vurdering = overgangUforevurderingRowMapper(it)
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
                     g.vurderinger_id,
                     g.opprettet_tid as grunnlag_opprettet_tid
            
            from overgang_ufore_grunnlag g 
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
                    vurderingerId = it.getLong("vurderinger_id"),
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
        val vurderingerId: Long,
        val grunnlagOpprettetTid: LocalDateTime,
    )
}