package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.lovvalgmedlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Arbeidsforholdtype
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntekterForKrav
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntekterINorgeRegisterGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.LovvalgMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemKode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemMedl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.LovvalgMedlemskapRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate

class LovvalgMedlemskapRepositoryImpl(private val connection: DBConnection) : LovvalgMedlemskapRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<LovvalgMedlemskapRepositoryImpl> {
        override fun konstruer(connection: DBConnection): LovvalgMedlemskapRepositoryImpl {
            return LovvalgMedlemskapRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): LovvalgMedlemskapGrunnlag? {
        val query = """
            SELECT * FROM MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapGrunnlag)
        }
    }

    override fun lagreVurderinger(
        behandlingId: BehandlingId,
        vurderinger: List<ManuellVurderingForLovvalgMedlemskap>
    ) {
        val grunnlagOppslag = hentGrunnlag(behandlingId)
        if (grunnlagOppslag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val vurderingerId = if (vurderinger.isNotEmpty()) lagreVurderinger(vurderinger) else null

        val grunnlagQuery = """
            INSERT INTO MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG 
            (behandling_id, arbeider_id, inntekter_i_norge_id, medlemskap_unntak_person_id, vurderinger_id) 
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, grunnlagOppslag?.arbeiderId)
                setLong(3, grunnlagOppslag?.inntektINorgeId)
                setLong(4, grunnlagOppslag?.medlId)
                setLong(5, vurderingerId)
            }
            setResultValidator { require(it == 1) }
        }
    }

    private fun lagreVurderinger(vurderinger: List<ManuellVurderingForLovvalgMedlemskap>): Long {
        val overstyrt = vurderinger.any { it.overstyrt }

        val vurderingerId = connection.executeReturnKey(
            """
            INSERT INTO lovvalg_medlemskap_manuell_vurderinger DEFAULT VALUES
        """.trimIndent()
        )

        val query = """
                INSERT INTO LOVVALG_MEDLEMSKAP_MANUELL_VURDERING 
                (fom, tom, tekstvurdering_lovvalg, lovvalgs_land, tekstvurdering_medlemskap, var_medlem_i_folketrygden, overstyrt, vurdert_av, opprettet_tid, vurdert_i_behandling, vurderinger_id) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        connection.executeBatch(query, vurderinger) {
            setParams { manuellVurdering ->
                setLocalDate(1, manuellVurdering.fom)
                setLocalDate(2, manuellVurdering.tom)
                setString(3, manuellVurdering.lovvalg.begrunnelse)
                setEnumName(4, manuellVurdering.lovvalg.lovvalgsEØSLandEllerLandMedAvtale)
                setString(5, manuellVurdering.medlemskap?.begrunnelse)
                setBoolean(6, manuellVurdering.medlemskap?.varMedlemIFolketrygd)
                setBoolean(7, overstyrt)
                setBruker(8, manuellVurdering.vurdertAv)
                setLocalDateTime(9, manuellVurdering.vurdertDato)
                setLong(10, manuellVurdering.vurdertIBehandling.toLong())
                setLong(11, vurderingerId)
            }
        }

        return vurderingerId
    }

    override fun lagreArbeidsforholdOgInntektINorge(
        behandlingId: BehandlingId,
        relevantDato: LocalDate,
        arbeidGrunnlag: List<ArbeidINorgeGrunnlag>,
        inntektGrunnlag: List<InntektINorgeGrunnlag>,
        medlId: Long?,
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val grunnlagOppslag = hentGrunnlag(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val arbeiderId = lagreArbeidGrunnlag(arbeidGrunnlag)
        val inntekterINorgeId = lagreArbeidsInntektGrunnlag(relevantDato, inntektGrunnlag)

        val grunnlagQuery = """
            INSERT INTO MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG (behandling_id, arbeider_id, inntekter_i_norge_id, medlemskap_unntak_person_id, vurderinger_id) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, arbeiderId)
                setLong(3, inntekterINorgeId)
                setLong(4, medlId)
                setLong(5, grunnlagOppslag?.vurderingerId)
            }
        }
    }

    private fun lagreArbeidGrunnlag(arbeidGrunnlag: List<ArbeidINorgeGrunnlag>): Long? {
        if (arbeidGrunnlag.isEmpty()) return null

        val arbeiderQuery = """
            INSERT INTO ARBEIDER DEFAULT VALUES
        """.trimIndent()
        val arbeiderId = connection.executeReturnKey(arbeiderQuery)

        for (forhold in arbeidGrunnlag) {
            val arbeidQuery = """
                INSERT INTO ARBEID (identifikator, arbeidsforhold_kode, arbeider_id, startdato, sluttdato) VALUES (?, ?, ?, ?, ?)
            """.trimIndent()

            connection.execute(arbeidQuery) {
                setParams {
                    setString(1, forhold.identifikator)
                    setString(2, forhold.arbeidsforholdKode.kode)
                    setLong(3, arbeiderId)
                    setLocalDate(4, forhold.startdato)
                    setLocalDate(5, forhold.sluttdato)
                }
            }
        }
        return arbeiderId
    }

    private fun lagreArbeidsInntektGrunnlag(
        relevantDato: LocalDate,
        inntekter: List<InntektINorgeGrunnlag>,
    ): Long? {
        if (inntekter.isEmpty()) return null

        val inntekterINorgeQuery = """
            INSERT INTO INNTEKTER_I_NORGE DEFAULT VALUES
        """.trimIndent()
        val inntekterINorgeId = connection.executeReturnKey(inntekterINorgeQuery)

        val inntektQuery = """
            INSERT INTO INNTEKT_I_NORGE (identifikator, beloep, skattemessig_bosatt_land, opptjenings_land, inntekt_type, inntekter_i_norge_id, periode, organisasjonsnavn, relevant_dato) VALUES (?, ?, ?, ?, ?, ?, ?::daterange, ?, ?)
        """.trimIndent()

        connection.executeBatch(
            inntektQuery,
            inntekter
        ) {
            setParams { inntekt ->
                setString(1, inntekt.identifikator)
                setDouble(2, inntekt.beloep)
                setString(3, inntekt.skattemessigBosattLand)
                setString(4, inntekt.opptjeningsLand)
                setString(5, inntekt.inntektType)
                setLong(6, inntekterINorgeId)
                setPeriode(7, inntekt.periode)
                setString(8, inntekt.organisasjonsNavn)
                setLocalDate(9, relevantDato)
            }
        }

        return inntekterINorgeId
    }

    private fun hentVurderinger(vurderingerId: Long?): List<ManuellVurderingForLovvalgMedlemskap> {
        if (vurderingerId == null) return emptyList()

        val query = """
            SELECT * FROM LOVVALG_MEDLEMSKAP_MANUELL_VURDERING WHERE VURDERINGER_ID = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper(::mapManuellVurderingForLovvalgMedlemskap)
        }
    }

    private fun hentMedlemskapGrunnlag(medlemskapId: Long?): MedlemskapUnntakGrunnlag? {
        if (medlemskapId == null) return null

        val data = connection.queryList(
            """SELECT * FROM MEDLEMSKAP_UNNTAK WHERE MEDLEMSKAP_UNNTAK_PERSON_ID = ?""".trimIndent()
        ) {
            setParams {
                setLong(1, medlemskapId)
            }
            setRowMapper {
                val unntak = Unntak(
                    status = it.getString("STATUS"),
                    statusaarsak = it.getStringOrNull("STATUS_ARSAK"),
                    medlem = it.getBoolean("MEDLEM"),
                    grunnlag = it.getString("GRUNNLAG"),
                    lovvalg = it.getString("LOVVALG"),
                    helsedel = it.getBoolean("HELSEDEL"),
                    lovvalgsland = it.getStringOrNull("LOVVALGSLAND"),
                    kilde = hentKildesystem(it)
                )
                Segment(
                    it.getPeriode("PERIODE"),
                    unntak
                )
            }
        }.toList()
        return MedlemskapUnntakGrunnlag(data)
    }

    private fun hentKildesystem(row: Row): KildesystemMedl? {
        val kildesystemKode: KildesystemKode? = row.getEnumOrNull("KILDESYSTEM")
        val kildeNavn = row.getStringOrNull("KILDENAVN")

        return if (kildesystemKode != null && kildeNavn != null) {
            KildesystemMedl(kildesystemKode, kildeNavn)
        } else null
    }

    private fun hentArbeiderINorgeGrunnlag(arbeiderINorgeId: Long?): List<ArbeidINorgeGrunnlag> {
        if (arbeiderINorgeId == null) return emptyList()

        val query = """
            SELECT * FROM ARBEID WHERE arbeider_id = ? AND arbeidsforhold_kode != 'maritimtArbeidsforhold'
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, arbeiderINorgeId)
            }
            setRowMapper {
                ArbeidINorgeGrunnlag(
                    identifikator = it.getString("identifikator"),
                    arbeidsforholdKode = Arbeidsforholdtype.fraKode(it.getString("arbeidsforhold_kode")),
                    startdato = it.getLocalDate("startdato"),
                    sluttdato = it.getLocalDateOrNull("sluttdato"),
                )
            }
        }
    }

    private fun hentInntekterINorgeGrunnlag(inntekterINorgeId: Long?): List<InntektINorgeInternal> {
        if (inntekterINorgeId == null) return emptyList()

        val query = """
            SELECT * FROM INNTEKT_I_NORGE WHERE inntekter_i_norge_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, inntekterINorgeId)
            }
            setRowMapper {
                InntektINorgeInternal(
                    identifikator = it.getString("identifikator"),
                    beloep = it.getDouble("beloep"),
                    skattemessigBosattLand = it.getStringOrNull("skattemessig_bosatt_land"),
                    opptjeningsLand = it.getStringOrNull("opptjenings_land"),
                    inntektType = it.getStringOrNull("inntekt_type"),
                    periode = it.getPeriode("periode"),
                    organisasjonsNavn = it.getStringOrNull("organisasjonsnavn"),
                    relevantDato = it.getLocalDateOrNull("relevant_dato")
                )
            }
        }
    }

    data class InntektINorgeInternal(
        val identifikator: String,
        val beloep: Double,
        val skattemessigBosattLand: String?,
        val opptjeningsLand: String?,
        val inntektType: String?,
        val periode: Periode,
        val organisasjonsNavn: String?,
        val relevantDato: LocalDate?,
    )

    private fun List<InntektINorgeInternal>.tilDomene(): List<InntektINorgeGrunnlag> {
        return this.map {
            InntektINorgeGrunnlag(
                identifikator = it.identifikator,
                beloep = it.beloep,
                skattemessigBosattLand = it.skattemessigBosattLand,
                opptjeningsLand = it.opptjeningsLand,
                inntektType = it.inntektType,
                periode = it.periode,
                organisasjonsNavn = it.organisasjonsNavn
            )
        }
    }

    private fun List<InntektINorgeInternal>.perKrav(): Set<InntekterForKrav> {
        return this
            .groupBy { it.relevantDato }
            .map { (relevantDato, inntekter) ->
                InntekterForKrav(
                    relevantDato = relevantDato,
                    inntekter = inntekter.tilDomene().toSet()
                )
            }.toSet()
    }

    private fun hentGrunnlag(behandlingId: BehandlingId): GrunnlagOppslag? {
        val query = """
            SELECT * FROM MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                GrunnlagOppslag(
                    it.getLongOrNull("medlemskap_unntak_person_id"),
                    it.getLongOrNull("inntekter_i_norge_id"),
                    it.getLongOrNull("arbeider_id"),
                    it.getLongOrNull("vurderinger_id")
                )
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        hentHvisEksisterer(fraBehandling) ?: return

        val query = """
            INSERT INTO MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG 
                (behandling_id, medlemskap_unntak_person_id, inntekter_i_norge_id, arbeider_id, vurderinger_id) 
            SELECT ?, medlemskap_unntak_person_id, inntekter_i_norge_id, arbeider_id, vurderinger_id
                from MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG 
                where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val inntekterINorgeIds = getInntekterINorgeIds(behandlingId)
        val lovvalgMedlemsskapManuellVurderingIds = getLovvalgMedlemsskapManuellVurderingIds(behandlingId)
        val arbeiderIds = getArbeiderIds(behandlingId)
        val arbeidIds = getArbeidIds(arbeiderIds)
        val inntektINorgeIds = getInntektINorgeIds(inntekterINorgeIds)


        val deletedRows = connection.executeReturnUpdated(
            """
            delete from MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG where behandling_id = ?; 
            delete from INNTEKT_I_NORGE where inntekter_i_norge_id = ANY(?::bigint[]);     
            delete from ARBEID where arbeider_id = ANY(?::bigint[]);
            delete from ARBEIDER where id = ANY(?::bigint[]);    
            delete from LOVVALG_MEDLEMSKAP_MANUELL_VURDERING where vurderinger_id = ANY(?::bigint[]);
            delete from LOVVALG_MEDLEMSKAP_MANUELL_VURDERINGER where id = ANY(?::bigint[]);
            delete from INNTEKTER_I_NORGE where id = ANY(?::bigint[]);
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, inntektINorgeIds)
                setLongArray(3, arbeidIds)
                setLongArray(4, arbeiderIds)
                setLongArray(5, lovvalgMedlemsskapManuellVurderingIds)
                setLongArray(6, lovvalgMedlemsskapManuellVurderingIds)
                setLongArray(7, inntektINorgeIds)
            }
        }
        log.info("Slettet $deletedRows rader fra MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG")
    }

    private fun getLovvalgMedlemsskapManuellVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT vurderinger_id
                    FROM MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG
                    WHERE behandling_id = ? AND vurderinger_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurderinger_id")
        }
    }

    private fun getInntekterINorgeIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT inntekter_i_norge_id
                    FROM MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG
                    WHERE behandling_id = ? AND inntekter_i_norge_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("inntekter_i_norge_id")
        }
    }

    private fun getArbeiderIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT arbeider_id
                    FROM MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG
                    WHERE behandling_id = ? AND arbeider_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("arbeider_id")
        }
    }

    private fun getInntektINorgeIds(inntekterId: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM INNTEKTER_I_NORGE
                    WHERE id = ANY(?::bigint[]);
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, inntekterId) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }

    private fun getArbeidIds(arbeiderIds: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM ARBEIDER
                    WHERE id = ANY(?::bigint[]);
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, arbeiderIds) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }

    private fun mapGrunnlag(row: Row): LovvalgMedlemskapGrunnlag {
        val inntekt = hentInntekterINorgeGrunnlag(row.getLongOrNull("inntekter_i_norge_id"))

        return LovvalgMedlemskapGrunnlag(
            medlemskapGrunnlag = hentMedlemskapGrunnlag(row.getLongOrNull("medlemskap_unntak_person_id")),
            inntekterINorgeGrunnlag = inntekt.tilDomene(),
            inntekterINorgeRegisterGrunnlag = InntekterINorgeRegisterGrunnlag(inntekt.perKrav()),
            arbeiderINorgeGrunnlag = hentArbeiderINorgeGrunnlag(row.getLongOrNull("arbeider_id")),
            vurderinger = hentVurderinger(row.getLongOrNull("vurderinger_id")),
        )
    }

    private fun mapManuellVurderingForLovvalgMedlemskap(row: Row): ManuellVurderingForLovvalgMedlemskap =
        ManuellVurderingForLovvalgMedlemskap(
            lovvalg = LovvalgDto(
                begrunnelse = row.getString("tekstvurdering_lovvalg"),
                lovvalgsEØSLandEllerLandMedAvtale = row.getEnum("lovvalgs_land")
            ),
            medlemskap = row.getStringOrNull("tekstvurdering_medlemskap")?.let { tekstvurderingMedlemskap ->
                MedlemskapDto(
                    begrunnelse = tekstvurderingMedlemskap,
                    varMedlemIFolketrygd = row.getBoolean("var_medlem_i_folketrygden")
                )
            },
            overstyrt = row.getBoolean("overstyrt"),
            vurdertAv = row.getBruker("vurdert_av"),
            vurdertDato = row.getLocalDateTime("opprettet_tid"),
            fom = row.getLocalDate("fom"),
            tom = row.getLocalDateOrNull("tom"),
            vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling"))
        )

    internal data class GrunnlagOppslag(
        val medlId: Long?,
        val inntektINorgeId: Long?,
        val arbeiderId: Long?,
        val vurderingerId: Long?
    )
}