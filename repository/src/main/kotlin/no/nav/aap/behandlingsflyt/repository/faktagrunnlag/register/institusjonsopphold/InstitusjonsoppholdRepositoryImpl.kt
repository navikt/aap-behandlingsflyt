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
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

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
                    vurdertIBehandling = BehandlingId(it.getLong("VURDERT_I_BEHANDLING"))
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
                    vurderinger = vurderingene,  // Sortert etter opphold og vurderingsperiode
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
        (HELSEOPPHOLD_VURDERINGER_ID, OPPHOLD_ID, KOST_OG_LOSJI, FORSORGER_EKTEFELLE, FASTE_UTGIFTER, BEGRUNNELSE, PERIODE, VURDERT_I_BEHANDLING) 
        VALUES (?, 
                (SELECT ID FROM OPPHOLD 
                 WHERE OPPHOLD_PERSON_ID = ?  
                 AND INSTITUSJONSTYPE = 'HS'
                 AND PERIODE @> ? ::daterange
                 ORDER BY PERIODE
                 LIMIT 1), 
                ?, ?, ?, ?, ? ::daterange, ?)
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
                setLong(9, vurdering.vurdertIBehandling.toLong())
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

        val vurderingerId = lagreHelseoppholdVurderinger(eksisterendeGrunnlag?.oppholdene?.id, helseinstitusjonVurderinger, vurdertAv)
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
        """. trimIndent()
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
            hv.VURDERT_I_BEHANDLING
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
                    oppholdPeriode = it. getPeriode("OPPHOLD_PERIODE"),
                    vurdering = HelseinstitusjonVurdering(
                        begrunnelse = it.getString("BEGRUNNELSE"),
                        faarFriKostOgLosji = it. getBoolean("KOST_OG_LOSJI"),
                        forsoergerEktefelle = it.getBooleanOrNull("FORSORGER_EKTEFELLE"),
                        harFasteUtgifter = it.getBooleanOrNull("FASTE_UTGIFTER"),
                        periode = it.getPeriode("VURDERING_PERIODE"),
                        vurdertIBehandling = BehandlingId(it.getLong("vurdert_i_behandling")),
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
        val vurdering:  HelseinstitusjonVurdering
    )

    internal data class Keychain(val oppholdId: Long?, val soningvurderingId: Long?, val helsevurderingId: Long?)
}