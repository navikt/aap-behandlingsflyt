package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLand
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LandMedAvtale
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemKode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemMedl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory

class MedlemskapArbeidInntektRepositoryImpl(private val connection: DBConnection) : MedlemskapArbeidInntektRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<MedlemskapArbeidInntektRepositoryImpl> {
        override fun konstruer(connection: DBConnection): MedlemskapArbeidInntektRepositoryImpl {
            return MedlemskapArbeidInntektRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MedlemskapArbeidInntektGrunnlag? {
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

    override fun hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId: BehandlingId): UtenlandsOppholdData? {
        val query = """
            SELECT * FROM OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                hentOppgittUtenlandsOpphold(it.getLong("oppgitt_utenlandsopphold_id"))
            }
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
                setEnumName(4, manuellVurdering.lovvalg.lovvalgsEØSLand as Enum<*>?)
                setString(5, manuellVurdering.medlemskap?.begrunnelse)
                setBoolean(6, manuellVurdering.medlemskap?.varMedlemIFolketrygd)
                setBoolean(7, overstyrt)
                setString(8, manuellVurdering.vurdertAv)
                setLocalDateTime(9, manuellVurdering.vurdertDato)
                setLong(10, manuellVurdering.vurdertIBehandling.toLong())
                setLong(11, vurderingerId)
            }
        }

        return vurderingerId
    }

    override fun lagreArbeidsforholdOgInntektINorge(
        behandlingId: BehandlingId,
        arbeidGrunnlag: List<ArbeidINorgeGrunnlag>,
        inntektGrunnlag: List<ArbeidsInntektMaaned>,
        medlId: Long?,
        enhetGrunnlag: List<EnhetGrunnlag>
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val grunnlagOppslag = hentGrunnlag(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val arbeiderId = lagreArbeidGrunnlag(arbeidGrunnlag)
        val inntekterINorgeId = lagreArbeidsInntektGrunnlag(inntektGrunnlag, enhetGrunnlag)

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
                    setString(2, forhold.arbeidsforholdKode)
                    setLong(3, arbeiderId)
                    setLocalDate(4, forhold.startdato)
                    setLocalDate(5, forhold.sluttdato)
                }
            }
        }
        return arbeiderId
    }

    private fun lagreArbeidsInntektGrunnlag(
        arbeidsInntektGrunnlag: List<ArbeidsInntektMaaned>,
        enhetGrunnlag: List<EnhetGrunnlag>
    ): Long? {
        if (arbeidsInntektGrunnlag.isEmpty()) return null

        val inntekterINorgeQuery = """
            INSERT INTO INNTEKTER_I_NORGE DEFAULT VALUES
        """.trimIndent()
        val inntekterINorgeId = connection.executeReturnKey(inntekterINorgeQuery)

        val inntektQuery = """
            INSERT INTO INNTEKT_I_NORGE (identifikator, beloep, skattemessig_bosatt_land, opptjenings_land, inntekt_type, inntekter_i_norge_id, periode, organisasjonsnavn) VALUES (?, ?, ?, ?, ?, ?, ?::daterange, ?)
        """.trimIndent()

        connection.executeBatch(
            inntektQuery,
            arbeidsInntektGrunnlag.flatMap {
                it.arbeidsInntektInformasjon.inntektListe.map { inntekt ->
                    Pair(
                        it.aarMaaned,
                        inntekt
                    )
                }
            }
        ) {
            setParams { (årMåned, inntekt) ->
                val orgNavn = enhetGrunnlag.firstOrNull { it.orgnummer == inntekt.virksomhet.identifikator }?.orgNavn
                val tomFallback = inntekt.opptjeningsperiodeFom ?: årMåned.atDay(1)

                setString(1, inntekt.virksomhet.identifikator)
                setDouble(2, inntekt.beloep)
                setString(3, inntekt.skattemessigBosattLand)
                setString(4, inntekt.opptjeningsland)
                setString(5, inntekt.beskrivelse)
                setLong(6, inntekterINorgeId)
                setPeriode(
                    7,
                    Periode(
                        inntekt.opptjeningsperiodeFom ?: årMåned.atDay(1),
                        inntekt.opptjeningsperiodeTom ?: tomFallback
                    )
                )
                setString(8, orgNavn)
            }
        }

        return inntekterINorgeId
    }

    override fun lagreOppgittUtenlandsOppplysninger(
        behandlingId: BehandlingId,
        journalpostId: JournalpostId,
        utenlandsOppholdData: UtenlandsOppholdData
    ) {
        val eksisterendeGrunnlag = hentOppgittUtenlandsOppholdHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverUtenlandsOppholdGrunnlag(behandlingId)
        }

        val oppgittUtenlandsOppholdQuery = """
            INSERT INTO OPPGITT_UTENLANDSOPPHOLD (BODD_I_NORGE_SISTE_FEM_AAR, ARBEIDET_I_NORGE_SISTE_FEM_AAR, ARBEIDET_UTENFOR_NORGE_FOR_SYKDOM, I_TILLEGG_ARBEID_UTENFOR_NORGE) VALUES (?, ?, ?, ?)
        """.trimIndent()

        val oppgittUtenlandsOppholdId = connection.executeReturnKey(oppgittUtenlandsOppholdQuery) {
            setParams {
                setBoolean(1, utenlandsOppholdData.harBoddINorgeSiste5År)
                setBoolean(2, utenlandsOppholdData.harArbeidetINorgeSiste5År)
                setBoolean(3, utenlandsOppholdData.arbeidetUtenforNorgeFørSykdom)
                setBoolean(4, utenlandsOppholdData.iTilleggArbeidUtenforNorge)
            }
        }

        val oppgittPeriodeQuery = """
            INSERT INTO UTENLANDS_PERIODE (LAND, TIL_DATO, FRA_DATO, I_ARBEID, UTENLANDS_ID, OPPGITT_UTENLANDSOPPHOLD_ID) VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        if (!utenlandsOppholdData.utenlandsOpphold.isNullOrEmpty()) {
            for (opphold in utenlandsOppholdData.utenlandsOpphold!!) {
                connection.execute(oppgittPeriodeQuery) {
                    setParams {
                        setString(1, opphold.land)
                        setLocalDate(2, opphold.tilDato)
                        setLocalDate(3, opphold.fraDato)
                        setBoolean(4, opphold.iArbeid)
                        setString(5, opphold.utenlandsId)
                        setLong(6, oppgittUtenlandsOppholdId)
                    }
                }
            }
        }

        val oppgittUtenlandsOppholdGrunnlagQuery = """
            INSERT INTO OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG (BEHANDLING_ID, JOURNALPOST_ID, OPPGITT_UTENLANDSOPPHOLD_ID) VALUES (?, ?, ?)
        """.trimIndent()

        connection.execute(oppgittUtenlandsOppholdGrunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, journalpostId.identifikator)
                setLong(3, oppgittUtenlandsOppholdId)
            }
        }
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

    private fun hentOppgittUtenlandsOpphold(oppgittUtenlandsOppholdId: Long): UtenlandsOppholdData {
        val utenlandsPeriode = connection.queryList(
            """SELECT * FROM UTENLANDS_PERIODE WHERE OPPGITT_UTENLANDSOPPHOLD_ID = ?""".trimIndent()
        ) {
            setParams {
                setLong(1, oppgittUtenlandsOppholdId)
            }
            setRowMapper {
                UtenlandsPeriode(
                    land = it.getStringOrNull("land"),
                    tilDato = it.getLocalDateOrNull("til_dato"),
                    fraDato = it.getLocalDateOrNull("fra_dato"),
                    iArbeid = it.getBoolean("i_arbeid"),
                    utenlandsId = it.getStringOrNull("utenlands_id")
                )
            }
        }

        return connection.queryFirst(
            """SELECT * FROM OPPGITT_UTENLANDSOPPHOLD WHERE ID = ?""".trimIndent()
        ) {
            setParams {
                setLong(1, oppgittUtenlandsOppholdId)
            }
            setRowMapper {
                UtenlandsOppholdData(
                    harBoddINorgeSiste5År = it.getBoolean("bodd_i_norge_siste_fem_aar"),
                    harArbeidetINorgeSiste5År = it.getBoolean("arbeidet_i_norge_siste_fem_aar"),
                    arbeidetUtenforNorgeFørSykdom = it.getBoolean("arbeidet_utenfor_norge_for_sykdom"),
                    iTilleggArbeidUtenforNorge = it.getBoolean("i_tillegg_arbeid_utenfor_norge"),
                    utenlandsOpphold = utenlandsPeriode
                )
            }
        }
    }

    override fun hentSistRelevanteOppgitteUtenlandsOppholdHvisEksisterer(sakId: SakId): UtenlandsOppholdData? {
        val query = """
            SELECT *
            FROM OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG grunnlag
            JOIN BEHANDLING behandling ON grunnlag.BEHANDLING_ID = behandling.ID
            WHERE grunnlag.AKTIV AND behandling.SAK_ID = ?
            ORDER BY behandling.OPPRETTET_TID DESC
            LIMIT 1
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, sakId.id)
            }
            setRowMapper {
                hentOppgittUtenlandsOpphold(it.getLong("oppgitt_utenlandsopphold_id"))
            }
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
            SELECT * FROM ARBEID WHERE arbeider_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, arbeiderINorgeId)
            }
            setRowMapper {
                ArbeidINorgeGrunnlag(
                    identifikator = it.getString("identifikator"),
                    arbeidsforholdKode = it.getString("arbeidsforhold_kode"),
                    startdato = it.getLocalDate("startdato"),
                    sluttdato = it.getLocalDateOrNull("sluttdato"),
                )
            }
        }
    }

    private fun hentInntekterINorgeGrunnlag(inntekterINorgeId: Long?): List<InntektINorgeGrunnlag> {
        if (inntekterINorgeId == null) return emptyList()

        val query = """
            SELECT * FROM INNTEKT_I_NORGE WHERE inntekter_i_norge_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, inntekterINorgeId)
            }
            setRowMapper {
                InntektINorgeGrunnlag(
                    identifikator = it.getString("identifikator"),
                    beloep = it.getDouble("beloep"),
                    skattemessigBosattLand = it.getStringOrNull("skattemessig_bosatt_land"),
                    opptjeningsLand = it.getStringOrNull("opptjenings_land"),
                    inntektType = it.getStringOrNull("inntekt_type"),
                    periode = it.getPeriode("periode"),
                    organisasjonsNavn = it.getStringOrNull("organisasjonsnavn"),
                )
            }
        }
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

    private fun deaktiverUtenlandsOppholdGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE  OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
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
        val utenlandsOppholdIds = getUtenlandsOppholdIds(behandlingId)
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
            delete from UTENLANDS_PERIODE where oppgitt_utenlandsopphold_id = ANY(?::bigint[]);
            delete from OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG where behandling_id = ?; 
            delete from OPPGITT_UTENLANDSOPPHOLD where id = ANY(?::bigint[]); 
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
                setLongArray(7, utenlandsOppholdIds)
                setLong(8, behandlingId.id)
                setLongArray(9, utenlandsOppholdIds)
                setLongArray(10, inntektINorgeIds)
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

    private fun getUtenlandsOppholdIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }

    private fun mapGrunnlag(row: Row): MedlemskapArbeidInntektGrunnlag {
        return MedlemskapArbeidInntektGrunnlag(
            medlemskapGrunnlag = hentMedlemskapGrunnlag(row.getLongOrNull("medlemskap_unntak_person_id")),
            inntekterINorgeGrunnlag = hentInntekterINorgeGrunnlag(row.getLongOrNull("inntekter_i_norge_id")),
            arbeiderINorgeGrunnlag = hentArbeiderINorgeGrunnlag(row.getLongOrNull("arbeider_id")),
            vurderinger = hentVurderinger(row.getLongOrNull("vurderinger_id")),
        )
    }

    private fun mapManuellVurderingForLovvalgMedlemskap(row: Row): ManuellVurderingForLovvalgMedlemskap =
        ManuellVurderingForLovvalgMedlemskap(
            lovvalg = LovvalgDto(
                begrunnelse = row.getString("tekstvurdering_lovvalg"),
                lovvalgsEØSLand = row.getString("lovvalgs_land").let { code ->
                    enumValues<EØSLand>().find { it.name == code }
                        ?: enumValues<LandMedAvtale>().find { it.name == code }
                        ?: throw IllegalArgumentException("Ukjent landkode: $code")
                }
            ),
            medlemskap = row.getStringOrNull("tekstvurdering_medlemskap")?.let { tekstvurdering_medlemskap ->
                MedlemskapDto(
                    begrunnelse = tekstvurdering_medlemskap,
                    varMedlemIFolketrygd = row.getBoolean("var_medlem_i_folketrygden")
                )
            },
            overstyrt = row.getBoolean("overstyrt"),
            vurdertAv = row.getString("vurdert_av"),
            vurdertDato = row.getLocalDateTime("opprettet_tid"),
            fom = row.getLocalDate("fom"),
            tom = row.getLocalDateOrNull("tom"),
            vurdertIBehandling = row.getLong("vurdert_i_behandling").let { BehandlingId(it) }
        )

    internal data class GrunnlagOppslag(
        val medlId: Long?,
        val inntektINorgeId: Long?,
        val arbeiderId: Long?,
        val vurderingerId: Long?
    )
}