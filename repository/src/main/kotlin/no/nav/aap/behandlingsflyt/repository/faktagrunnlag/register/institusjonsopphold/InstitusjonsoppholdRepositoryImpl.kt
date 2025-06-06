package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Segment
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
            hentHelseoppholdvurderinger(keychain.helsevurderingId)
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

        return Soningsvurderinger(id = soningsvurderingerId, vurderinger = vurderingene)
    }

    private fun hentHelseoppholdvurderinger(helseoppholdId: Long?): Helseoppholdvurderinger? {
        if (helseoppholdId == null) {
            return null
        }

        val vurderingene = connection.queryList(
            """
                SELECT * FROM HELSEOPPHOLD_VURDERING WHERE HELSEOPPHOLD_VURDERINGER_ID =?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, helseoppholdId)
            }
            setRowMapper { row ->
                HelseinstitusjonVurdering(
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    faarFriKostOgLosji = row.getBoolean("KOST_OG_LOSJI"),
                    forsoergerEktefelle = row.getBooleanOrNull("FORSORGER_EKTEFELLE"),
                    harFasteUtgifter = row.getBooleanOrNull("FASTE_UTGIFTER"),
                    periode = row.getPeriode("PERIODE")
                )
            }
        }.toList()

        return Helseoppholdvurderinger(id = helseoppholdId, vurderinger = vurderingene)
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

    override fun lagreSoningsVurdering(behandlingId: BehandlingId, soningsvurderinger: List<Soningsvurdering>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterendeGrunnlag(behandlingId)
        }

        val vurderingerId = lagreSoningsVurderinger(soningsvurderinger)
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

    private fun lagreSoningsVurderinger(soningsvurderings: List<Soningsvurdering>): Long? {
        if (soningsvurderings.isEmpty()) {
            return null
        }

        val vurderingerId = connection.executeReturnKey(
            """
            INSERT INTO SONING_VURDERINGER DEFAULT VALUES
        """.trimIndent()
        )

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

    private fun lagreHelseoppholdVurderinger(helseoppholdVurderinger: List<HelseinstitusjonVurdering>): Long? {
        if (helseoppholdVurderinger.isEmpty()) {
            return null
        }

        val vurderingerId = connection.executeReturnKey(
            """
            INSERT INTO HELSEOPPHOLD_VURDERINGER DEFAULT VALUES
        """.trimIndent()
        )

        val query = """
            INSERT INTO HELSEOPPHOLD_VURDERING 
            (HELSEOPPHOLD_VURDERINGER_ID, KOST_OG_LOSJI, FORSORGER_EKTEFELLE, FASTE_UTGIFTER, BEGRUNNELSE, PERIODE) 
            VALUES 
            (?, ?, ?, ?, ?, ?::daterange)
        """.trimIndent()

        connection.executeBatch(query, helseoppholdVurderinger) {
            setParams {
                setLong(1, vurderingerId)
                setBoolean(2, it.faarFriKostOgLosji)
                setBoolean(3, it.forsoergerEktefelle)
                setBoolean(4, it.harFasteUtgifter)
                setString(5, it.begrunnelse)
                setPeriode(6, it.periode)
            }
        }

        return vurderingerId
    }

    override fun lagreHelseVurdering(
        behandlingId: BehandlingId,
        helseinstitusjonVurderinger: List<HelseinstitusjonVurdering>
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterendeGrunnlag(behandlingId)
        }

        val vurderingerId = lagreHelseoppholdVurderinger(helseinstitusjonVurderinger)
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

        val deletedRows = connection.executeReturnUpdated("""
            delete from opphold_grunnlag where behandling_id = ?; 
            delete from helseopphold_vurdering where helseopphold_vurderinger_id = ANY(?::bigint[]);
            delete from helseopphold_vurderinger where id = ANY(?::bigint[]);
            delete from soning_vurdering where soning_vurderinger_id = ANY(?::bigint[]);
            delete from soning_vurderinger where id = ANY(?::bigint[]);
            delete from opphold where opphold_person_id = ANY(?::bigint[]);
            delete from opphold_person where id = ANY(?::bigint[]);
        """.trimIndent()) {
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

    internal data class Keychain(val oppholdId: Long?, val soningvurderingId: Long?, val helsevurderingId: Long?)
}