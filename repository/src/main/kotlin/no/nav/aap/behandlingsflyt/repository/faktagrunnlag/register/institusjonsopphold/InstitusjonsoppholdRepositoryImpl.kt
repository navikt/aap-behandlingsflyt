package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Helseoppholdvurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdene
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Soningsvurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

class InstitusjonsoppholdRepositoryImpl(private val connection: DBConnection) :
    InstitusjonsoppholdRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<InstitusjonsoppholdRepository> {
        override fun konstruer(connection: DBConnection): InstitusjonsoppholdRepository {
            return InstitusjonsoppholdRepositoryImpl(connection)
        }
    }

    private fun hentOpphold(oppholdId: Long?): Oppholdene? {
        if (oppholdId == null) {
            return null
        }

        return Oppholdene(
            id = oppholdId, opphold = connection.queryList(
                """
                SELECT * FROM OPPHOLD WHERE OPPHOLD_PERSON_ID =?
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, oppholdId)
                }
                setRowMapper {
                    val institusjonsopphold = Institusjon(
                        Institusjonstype.valueOf(it.getString("INSTITUSJONSTYPE")),
                        Oppholdstype.valueOf(it.getString("KATEGORI")),
                        it.getString("ORGNR"),
                        it.getString("INSTITUSJONSNAVN")
                    )
                    Segment(
                        it.getPeriode("PERIODE"), institusjonsopphold
                    )
                }
            }.toList()
        )
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag? {
        val keychain = connection.queryFirstOrNull(
            "SELECT OPPHOLD_PERSON_ID, soning_vurderinger_id, HELSEOPPHOLD_VURDERINGER_ID FROM OPPHOLD_GRUNNLAG WHERE BEHANDLING_ID=? AND AKTIV=TRUE"
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                Keychain(
                    it.getLongOrNull("OPPHOLD_PERSON_ID"),
                    it.getLongOrNull("soning_vurderinger_id"),
                    it.getLongOrNull("HELSEOPPHOLD_VURDERINGER_ID")
                )
            }
        }
        if (keychain == null) {
            return null
        }
        return InstitusjonsoppholdGrunnlag(
            hentOpphold(keychain.oppholdId),
            hentSoningsvurderinger(keychain.soningvurderingId),
            hentHelseoppholdVurderinger(keychain.helsevurderingId)
        )
    }

    private fun hentSoningsvurderinger(soningsvurderingerId: Long?): Soningsvurderinger? {
        if (soningsvurderingerId == null) {
            return null
        }

        val vurderingene = connection.queryList(
            """
                SELECT * FROM SONING_VURDERING WHERE SONING_VURDERINGER_ID =?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, soningsvurderingerId)
            }
            setRowMapper { row ->
                Soningsvurdering(
                    skalOpphøre = row.getBoolean("SKAL_OPPHORE"),
                    fraDato = row.getLocalDate("FRA_DATO"),
                    begrunnelse = row.getString("BEGRUNNELSE")
                )
            }
        }.toList()

        return connection.queryFirst(
            """
            SELECT * FROM SONING_VURDERINGER WHERE ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, soningsvurderingerId)
            }
            setRowMapper { row ->
                Soningsvurderinger(
                    id = soningsvurderingerId,
                    vurderinger = vurderingene,
                    vurdertAv = row.getString("VURDERT_AV"),
                    vurdertTidspunkt = row.getLocalDateTime("OPPRETTET_TID")
                )
            }
        }
    }

    private fun hentHelseoppholdVurderinger(helseoppholdId: Long?): Helseoppholdvurderinger? {
        if (helseoppholdId == null) {
            return null
        }

        val vurderingene = connection.queryList(
            """
                SELECT hv.*, o.ID as OPPHOLD_ID, o. PERIODE as OPPHOLD_PERIODE, o.INSTITUSJONSNAVN
                FROM HELSEOPPHOLD_VURDERING hv
                INNER JOIN OPPHOLD o ON hv.OPPHOLD_ID = o.ID
                WHERE hv.HELSEOPPHOLD_VURDERINGER_ID = ? 
                ORDER BY o.PERIODE, hv.PERIODE
                """.trimIndent()
        ) {
            setParams {
                setLong(1, helseoppholdId)
            }
            setRowMapper {
                HelseinstitusjonVurdering(
                    begrunnelse = it.getString("BEGRUNNELSE"),
                    faarFriKostOgLosji = it.getBoolean("KOST_OG_LOSJI"),
                    forsoergerEktefelle = it.getBoolean("FORSORGER_EKTEFELLE"),
                    harFasteUtgifter = it.getBoolean("FASTE_UTGIFTER"),
                    periode = it.getPeriode("PERIODE"),
                    vurdertIBehandling = it.getLongOrNull("VURDERT_I_BEHANDLING")?.let { id -> BehandlingId(id) },
                    vurdertAv = it.getString("VURDERT_AV"),
                    vurdertTidspunkt = it.getLocalDateTime("OPPRETTET_TID")
                )
            }
        }

        return connection.queryFirst(
            """
        SELECT * FROM HELSEOPPHOLD_VURDERINGER WHERE ID = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, helseoppholdId)
            }
            setRowMapper { row ->
                Helseoppholdvurderinger(
                    id = helseoppholdId,
                    vurderinger = vurderingene,
                    vurdertAv = row.getString("VURDERT_AV"),
                    vurdertTidspunkt = row.getLocalDateTime("OPPRETTET_TID")
                )
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    override fun lagreOpphold(behandlingId: BehandlingId, institusjonsopphold: List<Institusjonsopphold>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterendeGrunnlag(behandlingId)
        }
        val oppholdPersonId = connection.executeReturnKey(
            """
            INSERT INTO OPPHOLD_PERSON DEFAULT VALUES
        """.trimIndent()
        )

        connection.executeBatch(
            """
                INSERT INTO OPPHOLD (INSTITUSJONSTYPE, KATEGORI, ORGNR, PERIODE, OPPHOLD_PERSON_ID, INSTITUSJONSNAVN) VALUES (?, ?, ?, ?::daterange, ?, ?)
            """.trimIndent(), institusjonsopphold
        ) {
            setParams { opphold ->
                setString(1, opphold.institusjonstype.name)
                setString(2, opphold.kategori.name)
                setString(3, opphold.orgnr)
                setPeriode(4, opphold.periode())
                setLong(5, oppholdPersonId)
                setString(6, opphold.institusjonsnavn)
            }
        }

        connection.execute(
            """
            INSERT INTO OPPHOLD_GRUNNLAG (BEHANDLING_ID, OPPHOLD_PERSON_ID, soning_vurderinger_id, HELSEOPPHOLD_VURDERINGER_ID) VALUES (?, ?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, oppholdPersonId)
                setLong(3, eksisterendeGrunnlag?.soningsVurderinger?.id)
                setLong(4, eksisterendeGrunnlag?.helseoppholdvurderinger?.id)
            }
        }
    }

    override fun lagreSoningsVurdering(
        behandlingId: BehandlingId,
        vurdertAv: String,
        soningsvurderinger: List<Soningsvurdering>
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterendeGrunnlag(behandlingId)
        }

        val vurderingerId = lagreSoningsVurderinger(soningsvurderinger, vurdertAv)
        connection.execute(
            """
            INSERT INTO OPPHOLD_GRUNNLAG (BEHANDLING_ID, OPPHOLD_PERSON_ID, soning_vurderinger_id, HELSEOPPHOLD_VURDERINGER_ID) VALUES (?, ?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, eksisterendeGrunnlag?.oppholdene?.id)
                setLong(3, vurderingerId)
                setLong(4, eksisterendeGrunnlag?.helseoppholdvurderinger?.id)
            }
        }
    }

    private fun lagreSoningsVurderinger(soningsvurderings: List<Soningsvurdering>, vurdertAv: String): Long? {
        if (soningsvurderings.isEmpty()) {
            return null
        }

        val vurderingerId =
            connection.executeReturnKey(
                """
                  INSERT INTO SONING_VURDERINGER (VURDERT_AV)
                VALUES (?)
                """.trimIndent()
            ) {
                setParams {
                    setString(1, vurdertAv)
                }
            }

        val query = """
            INSERT INTO SONING_VURDERING (SONING_VURDERINGER_ID, SKAL_OPPHORE, BEGRUNNELSE, FRA_DATO) VALUES (?, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(query, soningsvurderings) {
            setParams {
                setLong(1, vurderingerId)
                setBoolean(2, it.skalOpphøre)
                setString(3, it.begrunnelse)
                setLocalDate(4, it.fraDato)
            }
        }

        return vurderingerId
    }

    private fun lagreHelseoppholdVurderinger(
        oppholdPersonId: Long?,
        helseoppholdVurderinger: List<HelseinstitusjonVurdering>,
        vurdertAv: String
    ): Long? {
        if (helseoppholdVurderinger.isEmpty()) {
            return null
        }

        requireNotNull(oppholdPersonId) { "OPPHOLD_PERSON_ID må være satt før helseoppholdvurderinger kan lagres" }

        // Valider at alle vurderinger matcher et opphold
        helseoppholdVurderinger.forEach { vurdering ->
            val oppholdFinnes = connection.queryFirstOrNull(
                """
            SELECT ID FROM OPPHOLD 
            WHERE OPPHOLD_PERSON_ID = ?   
            AND INSTITUSJONSTYPE = 'HS'
            AND PERIODE @> ?:: daterange
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, oppholdPersonId)
                    setPeriode(2, vurdering.periode)
                }
                setRowMapper { it.getLong("ID") }
            }

            require(oppholdFinnes != null) {
                "Ingen helseinstitusjon-opphold funnet for periode ${vurdering.periode.fom} - ${vurdering.periode.tom}.  " +
                        "Vurderingen kan ikke lagres."
            }
        }

        val vurderingerId = connection.executeReturnKey(
            """
                INSERT INTO HELSEOPPHOLD_VURDERINGER (VURDERT_AV)
                VALUES (?)
            """.trimIndent()
        ) {
            setParams {
                setString(1, vurdertAv)
            }
        }
        val query = """
        INSERT INTO HELSEOPPHOLD_VURDERING 
        (HELSEOPPHOLD_VURDERINGER_ID, OPPHOLD_ID, KOST_OG_LOSJI, FORSORGER_EKTEFELLE, FASTE_UTGIFTER, BEGRUNNELSE, PERIODE, VURDERT_I_BEHANDLING, VURDERT_AV) 
        VALUES (?, 
                (SELECT ID FROM OPPHOLD 
                 WHERE OPPHOLD_PERSON_ID = ?  
                 AND INSTITUSJONSTYPE = 'HS'
                 AND PERIODE @> ? ::daterange
                 ORDER BY PERIODE
                 LIMIT 1), 
                ?, ?, ?, ?, ? ::daterange, ?, ?)
         """.trimIndent()

        connection.executeBatch(query, helseoppholdVurderinger) {
            setParams { vurdering ->
                setLong(1, vurderingerId)
                setLong(2, oppholdPersonId)
                setPeriode(3, vurdering.periode)
                setBoolean(4, vurdering.faarFriKostOgLosji)
                setBoolean(5, vurdering.forsoergerEktefelle)
                setBoolean(6, vurdering.harFasteUtgifter)
                setString(7, vurdering.begrunnelse)
                setPeriode(8, vurdering.periode)
                setLong(9, vurdering.vurdertIBehandling?.toLong())
                setString(10, vurdertAv)
            }
        }

        return vurderingerId
    }

    override fun lagreHelseVurdering(
        behandlingId: BehandlingId,
        vurdertAv: String,
        helseinstitusjonVurderinger: List<HelseinstitusjonVurdering>
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterendeGrunnlag(behandlingId)
        }

        val vurderingerId =
            lagreHelseoppholdVurderinger(eksisterendeGrunnlag?.oppholdene?.id, helseinstitusjonVurderinger, vurdertAv)
        connection.execute(
            """
            INSERT INTO OPPHOLD_GRUNNLAG (BEHANDLING_ID, OPPHOLD_PERSON_ID, soning_vurderinger_id, HELSEOPPHOLD_VURDERINGER_ID) VALUES (?, ?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, eksisterendeGrunnlag?.oppholdene?.id)
                setLong(3, eksisterendeGrunnlag?.soningsVurderinger?.id)
                setLong(4, vurderingerId)
            }
        }
    }

    private fun deaktiverEksisterendeGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE OPPHOLD_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    override fun hentVurderingerGruppertPerOpphold(behandlingId: BehandlingId): Map<Periode, List<HelseinstitusjonVurdering>> {
        // Hent HELSEOPPHOLD_VURDERINGER_ID
        val helseoppholdVurderingerId = connection.queryFirstOrNull(
            """
        SELECT HELSEOPPHOLD_VURDERINGER_ID 
        FROM OPPHOLD_GRUNNLAG 
        WHERE BEHANDLING_ID = ? AND AKTIV = TRUE
        """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getLongOrNull("HELSEOPPHOLD_VURDERINGER_ID") }
        }

        if (helseoppholdVurderingerId == null) {
            return emptyMap()
        }

        // Hent vurderinger med oppholdsperiode
        val vurderingerMedOppholdPeriode = connection.queryList(
            """
        SELECT 
            o.PERIODE as OPPHOLD_PERIODE,
            hv.BEGRUNNELSE,
            hv.KOST_OG_LOSJI,
            hv.FORSORGER_EKTEFELLE,
            hv.FASTE_UTGIFTER,
            hv.PERIODE as VURDERING_PERIODE,
            hv.VURDERT_I_BEHANDLING,
            hv.VURDERT_AV,
            hv.OPPRETTET_TID
        FROM HELSEOPPHOLD_VURDERING hv
        INNER JOIN OPPHOLD o ON hv.OPPHOLD_ID = o.ID
        WHERE hv.HELSEOPPHOLD_VURDERINGER_ID = ?
        ORDER BY o.PERIODE, hv.PERIODE
        """.trimIndent()
        ) {
            setParams {
                setLong(1, helseoppholdVurderingerId)
            }
            setRowMapper {
                VurderingMedOppholdPeriode(
                    oppholdPeriode = it.getPeriode("OPPHOLD_PERIODE"),
                    vurdering = HelseinstitusjonVurdering(
                        begrunnelse = it.getString("BEGRUNNELSE"),
                        faarFriKostOgLosji = it.getBoolean("KOST_OG_LOSJI"),
                        forsoergerEktefelle = it.getBooleanOrNull("FORSORGER_EKTEFELLE"),
                        harFasteUtgifter = it.getBooleanOrNull("FASTE_UTGIFTER"),
                        periode = it.getPeriode("VURDERING_PERIODE"),
                        vurdertIBehandling = it.getLongOrNull("VURDERT_I_BEHANDLING")?.let { id -> BehandlingId(id) },
                        vurdertAv = it.getString("VURDERT_AV"),
                        vurdertTidspunkt = it.getLocalDateTime("OPPRETTET_TID")
                    )
                )
            }
        }

        // Grupper per oppholdsperiode
        return vurderingerMedOppholdPeriode.groupBy(
            { it.oppholdPeriode },
            { it.vurdering }
        )
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        val query =
            """INSERT INTO OPPHOLD_GRUNNLAG (BEHANDLING_ID, OPPHOLD_PERSON_ID, soning_vurderinger_id, HELSEOPPHOLD_VURDERINGER_ID) 
            SELECT ?, OPPHOLD_PERSON_ID, soning_vurderinger_id, HELSEOPPHOLD_VURDERINGER_ID 
            FROM OPPHOLD_GRUNNLAG 
            WHERE AKTIV AND BEHANDLING_ID = ?""".trimMargin()
        connection.execute(
            query
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val oppholdPersonIds = getOppholdPersonIds(behandlingId)
        val helseoppholdVurderingerIds = getHelseOppholdVurderingerIds(behandlingId)
        val soningVurderingerIds = getSoningVurderingerIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from opphold_grunnlag where behandling_id = ?; 
            delete from helseopphold_vurdering where helseopphold_vurderinger_id = ANY(?::bigint[]);
            delete from helseopphold_vurderinger where id = ANY(?::bigint[]);
            delete from soning_vurdering where soning_vurderinger_id = ANY(?::bigint[]);
            delete from soning_vurderinger where id = ANY(?::bigint[]);
            delete from opphold where opphold_person_id = ANY(?::bigint[]);
            delete from opphold_person where id = ANY(?::bigint[]);
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, helseoppholdVurderingerIds)
                setLongArray(3, helseoppholdVurderingerIds)
                setLongArray(4, soningVurderingerIds)
                setLongArray(5, soningVurderingerIds)
                setLongArray(6, oppholdPersonIds)
                setLongArray(7, oppholdPersonIds)
            }
        }
        log.info("Slettet $deletedRows rader fra opphold_grunnlag")
    }

    override fun migrerInstitusjonsopphold() {
        log.info("Starter migrering av institusjonsopphold")
        // Hent alle koblingstabeller
        val kandidater = hentKandidater()
        val kandidaterGruppertPåSak = kandidater.groupBy { it.sakId }

        var migrerteVurderingerCount = 0
        val totalTid = measureTimeMillis {
            kandidaterGruppertPåSak.forEach { (sakId, kandidaterForSak) ->
                log.info("Migrerer institusjonsopphold for sak ${sakId.id} med ${kandidaterForSak.size} kandidater")
                val sorterteKandidater = kandidaterForSak.sortedBy { it.grunnlagOpprettetTid }
                val vurderingerMedVurderingerId =
                    hentVurderinger(kandidaterForSak.mapNotNull { it.helseoppholdVurderingerId }.toSet().toList())

                // Kan skippe grunnlag som peker på vurderinger som allerede er migrert
                val migrerteVurderingerId = mutableSetOf<Long>()

                // Dette dekker en eksisterende vurdering som er lagret som en del av et nytt grunnlag;
                // disse har ikke samme id som den originale.
                // Antar at like vurderinger innenfor samme sak er samme vurdering
                val nyeVerdierForVurdering =
                    mutableMapOf<SammenlignbarHelseoppholdvurdering, BehandlingId>()

                sorterteKandidater.filterNot { it.helseoppholdVurderingerId in migrerteVurderingerId }.forEach { kandidat ->
                    val vurderingerForGrunnlag =
                        vurderingerMedVurderingerId.filter { it.vurderingerId == kandidat.helseoppholdVurderingerId }

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
                        UPDATE HELSEOPPHOLD_VURDERING
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

                        kandidat.helseoppholdVurderingerId?.let { migrerteVurderingerId.add(it) }
                    }
                }
            }
        }
        log.info("Fullført migrering av institusjonsopphold. Migrerte ${kandidater.size} grunnlag og ${migrerteVurderingerCount} vurderinger på $totalTid ms.")
    }

    // Vurdering minus opprettet, periode.tom, vurdertIBehandling
    data class SammenlignbarHelseoppholdvurdering(
        val begrunnelse: String,
        val faarFriKostOgLosji: Boolean,
        val forsoergerEktefelle: Boolean?,
        val harFasteUtgifter: Boolean?,
        val fraDato: LocalDate,
        val vurdertAv: String?,
    )

    private fun HelseinstitusjonVurdering.tilSammenlignbar(): SammenlignbarHelseoppholdvurdering {
        return SammenlignbarHelseoppholdvurdering(
            begrunnelse = this.begrunnelse,
            faarFriKostOgLosji = this.faarFriKostOgLosji,
            forsoergerEktefelle = this.forsoergerEktefelle,
            harFasteUtgifter = this.harFasteUtgifter,
            fraDato = this.periode.fom,
            vurdertAv = this.vurdertAv,
        )
    }

    private data class VurderingMedVurderingerId(
        val vurderingerId: Long,
        val vurderingId: Long,
        val vurdering: HelseinstitusjonVurdering
    )

    private fun hentVurderinger(vurderingerIds: List<Long>): List<VurderingMedVurderingerId> {
        val query = """
            select * from helseopphold_vurdering
            where helseopphold_vurderinger_id = ANY(?::bigint[])
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLongArray(1, vurderingerIds)
            }
            setRowMapper {
                VurderingMedVurderingerId(
                    vurderingerId = it.getLong("helseopphold_vurderinger_id"),
                    vurderingId = it.getLong("id"),
                    vurdering = toHelseoppholdInternal(it).toHelseinstitusjonVurdering()
                )
            }
        }
    }

    fun toHelseoppholdInternal(row: Row): HelseoppholdInternal = HelseoppholdInternal(
        helseoppholdVurderingerId = row.getLong("helseopphold_vurderinger_id"),
        kostOgLosji = row.getBoolean("KOST_OG_LOSJI"),
        forsoergerEktefelle = row.getBoolean("FORSORGER_EKTEFELLE"),
        harFasteUtgifter = row.getBoolean("FASTE_UTGIFTER"),
        begrunnelse = row.getString("BEGRUNNELSE"),
        periode = row.getPeriode("PERIODE"),
        opprettetTid = row.getLocalDateTime("OPPRETTET_TID"),
        oppholdId = row.getLongOrNull("OPPHOLD_ID"),
        vurdertIBehandling = row.getLongOrNull("VURDERT_I_BEHANDLING")?.let { BehandlingId(it) },
        vurdertAv = row.getString("VURDERT_AV")
    )

    data class HelseoppholdInternal(
        val helseoppholdVurderingerId: Long,
        val kostOgLosji: Boolean,
        val forsoergerEktefelle: Boolean,
        val harFasteUtgifter: Boolean,
        val begrunnelse: String,
        val periode: Periode,
        val opprettetTid: LocalDateTime,
        val oppholdId: Long?,
        val vurdertIBehandling: BehandlingId?,
        val vurdertAv: String
    ) {
        fun toHelseinstitusjonVurdering(): HelseinstitusjonVurdering {
            return HelseinstitusjonVurdering(
                begrunnelse = begrunnelse,
                faarFriKostOgLosji = kostOgLosji,
                forsoergerEktefelle = forsoergerEktefelle,
                harFasteUtgifter = harFasteUtgifter,
                periode = periode,
                vurdertIBehandling = vurdertIBehandling,
                vurdertAv = vurdertAv,
                vurdertTidspunkt = opprettetTid
            )
        }
    }

    private fun hentKandidater(): List<Kandidat> {
        val kandidaterQuery = """
            select g.id as grunnlag_id,
                     b.id as behandling_id,
                     s.id as sak_id,
                     s.rettighetsperiode,
                     g.helseopphold_vurderinger_id, 
                     g.opprettet_tid as grunnlag_opprettet_tid
            
            from opphold_grunnlag g 
            inner join behandling b on g.behandling_id = b.id
            inner join sak s on b.sak_id = s.id
            where g.helseopphold_vurderinger_id is not null
        """.trimIndent()

        return connection.queryList(kandidaterQuery) {
            setRowMapper {
                Kandidat(
                    sakId = SakId(it.getLong("sak_id")),
                    grunnlagId = it.getLong("grunnlag_id"),
                    behandlingId = BehandlingId(it.getLong("behandling_id")),
                    rettighetsperiode = it.getPeriode("rettighetsperiode"), // Denne er teknisk sett feil, men kanskje godt nok. Hvis ikke: join på rettighetsperiode_grunnlag
                    helseoppholdVurderingerId = it.getLongOrNull("helseopphold_vurderinger_id"),
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
        val helseoppholdVurderingerId: Long?, // samme som vurderinger_id i andre vilkår
        val grunnlagOpprettetTid: LocalDateTime,
    )

    private fun getOppholdPersonIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT opphold_person_id
                    FROM opphold_grunnlag
                    WHERE behandling_id = ? AND opphold_person_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("opphold_person_id")
        }
    }

    private fun getSoningVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT soning_vurderinger_id
                    FROM opphold_grunnlag
                    WHERE behandling_id = ? AND soning_vurderinger_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("soning_vurderinger_id")
        }
    }

    private fun getHelseOppholdVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT helseopphold_vurderinger_id
                    FROM opphold_grunnlag
                    WHERE behandling_id = ? AND helseopphold_vurderinger_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("helseopphold_vurderinger_id")
        }
    }

    private data class VurderingMedOppholdPeriode(
        val oppholdPeriode: Periode,
        val vurdering: HelseinstitusjonVurdering
    )

    internal data class Keychain(val oppholdId: Long?, val soningvurderingId: Long?, val helsevurderingId: Long?)
}