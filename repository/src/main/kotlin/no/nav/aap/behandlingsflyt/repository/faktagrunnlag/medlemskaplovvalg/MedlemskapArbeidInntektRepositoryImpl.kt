package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.HistoriskManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapVedSøknadsTidspunktDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemKode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemMedl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.historiskevurderinger.ÅpenPeriodeDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

class MedlemskapArbeidInntektRepositoryImpl(private val connection: DBConnection) : MedlemskapArbeidInntektRepository {
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
            setRowMapper {
                MedlemskapArbeidInntektGrunnlag(
                    medlemskapGrunnlag = hentMedlemskapGrunnlag(it.getLongOrNull("medlemskap_unntak_person_id")),
                    inntekterINorgeGrunnlag = hentInntekterINorgeGrunnlag(it.getLongOrNull("inntekter_i_norge_id")),
                    arbeiderINorgeGrunnlag = hentArbeiderINorgeGrunnlag(it.getLongOrNull("arbeider_id")),
                    manuellVurdering = hentManuellVurdering(it.getLongOrNull("manuell_vurdering_id"))
                )
            }
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

    override fun lagreManuellVurdering(
        behandlingId: BehandlingId,
        manuellVurdering: ManuellVurderingForLovvalgMedlemskap
    ) {
        val grunnlagOppslag = hentGrunnlag(behandlingId)
        val eksisterendeManuellVurdering = hentManuellVurdering(grunnlagOppslag?.manuellVurderingId)
        val overstyrt = manuellVurdering.overstyrt || eksisterendeManuellVurdering?.overstyrt == true

        if (grunnlagOppslag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val manuellVurderingQuery = """
            INSERT INTO LOVVALG_MEDLEMSKAP_MANUELL_VURDERING (tekstvurdering_lovvalg, lovvalgs_land, tekstvurdering_medlemskap, var_medlem_i_folketrygden, overstyrt, vurdert_av) VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val manuellVurderingId = connection.executeReturnKey(manuellVurderingQuery) {
            setParams {
                setString(1, manuellVurdering.lovvalgVedSøknadsTidspunkt.begrunnelse)
                setEnumName(2, manuellVurdering.lovvalgVedSøknadsTidspunkt.lovvalgsEØSLand)
                setString(3, manuellVurdering.medlemskapVedSøknadsTidspunkt?.begrunnelse)
                setBoolean(4, manuellVurdering.medlemskapVedSøknadsTidspunkt?.varMedlemIFolketrygd)
                setBoolean(5, overstyrt)
                setString(6, manuellVurdering.vurdertAv)
            }
        }

        val grunnlagQuery = """
            INSERT INTO MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG (behandling_id, arbeider_id, inntekter_i_norge_id, medlemskap_unntak_person_id, manuell_vurdering_id) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, grunnlagOppslag?.arbeiderId)
                setLong(3, grunnlagOppslag?.inntektINorgeId)
                setLong(4, grunnlagOppslag?.medlId)
                setLong(5, manuellVurderingId)
            }
            setResultValidator { require(it == 1) }
        }
    }

    override fun lagreArbeidsforholdOgInntektINorge(
        behandlingId: BehandlingId,
        arbeidGrunnlag: List<ArbeidINorgeGrunnlag>,
        inntektGrunnlag: List<ArbeidsInntektMaaned>,
        medlId: Long?
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val grunnlagOppslag = hentGrunnlag(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val arbeiderId = lagreArbeidGrunnlag(arbeidGrunnlag)
        val inntekterINorgeId = lagreArbeidsInntektGrunnlag(inntektGrunnlag)

        val grunnlagQuery = """
            INSERT INTO MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG (behandling_id, arbeider_id, inntekter_i_norge_id, medlemskap_unntak_person_id, manuell_vurdering_id) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, arbeiderId)
                setLong(3, inntekterINorgeId)
                setLong(4, medlId)
                setLong(5, grunnlagOppslag?.manuellVurderingId)
            }
        }
    }

    override fun hentHistoriskeVurderinger(sakId: SakId, behandlingId: BehandlingId): List<HistoriskManuellVurderingForLovvalgMedlemskap> {
        val query = """
            SELECT vurdering.*
            FROM MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG grunnlag
            INNER JOIN LOVVALG_MEDLEMSKAP_MANUELL_VURDERING vurdering ON grunnlag.MANUELL_VURDERING_ID = vurdering.ID
            JOIN BEHANDLING behandling ON grunnlag.BEHANDLING_ID = behandling.ID
            WHERE grunnlag.AKTIV AND behandling.SAK_ID = ? 
              AND behandling.opprettet_tid < (SELECT a.opprettet_tid from behandling a where id = ?)
        """.trimIndent()

        val vurderinger = connection.queryList(query) {
            setParams {
                setLong(1, sakId.id)
                setLong(2, behandlingId.id)
            }
            setRowMapper {
                InternalHistoriskManuellVurderingForLovvalgMedlemskap(
                    manuellVurdering = ManuellVurderingForLovvalgMedlemskap(
                        lovvalgVedSøknadsTidspunkt = LovvalgVedSøknadsTidspunktDto(
                            begrunnelse = it.getString("tekstvurdering_lovvalg"),
                            lovvalgsEØSLand = it.getEnumOrNull("lovvalgs_land")
                        ),
                        medlemskapVedSøknadsTidspunkt = MedlemskapVedSøknadsTidspunktDto(
                            begrunnelse = it.getStringOrNull("tekstvurdering_medlemskap"),
                            varMedlemIFolketrygd = it.getBooleanOrNull("var_medlem_i_folketrygden")
                        ),
                        overstyrt = it.getBoolean("overstyrt"),
                        vurdertAv = it.getString("vurdert_av"),
                        vurdertDato = it.getLocalDate("opprettet_tid")
                    ),
                    vurdertDato = it.getLocalDate("opprettet_tid")
                )
            }
        }.sortedBy { it.vurdertDato }

        return vurderinger.map {
            HistoriskManuellVurderingForLovvalgMedlemskap(
                it.vurdertDato,
                it.manuellVurdering.vurdertAv,
                it == vurderinger.last(),
                periode = ÅpenPeriodeDto(it.vurdertDato),
                vurdering = it.manuellVurdering
            )
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

    private fun lagreArbeidsInntektGrunnlag(arbeidsInntektGrunnlag: List<ArbeidsInntektMaaned>): Long? {
        if (arbeidsInntektGrunnlag.isEmpty()) return null

        val inntekterINorgeQuery = """
            INSERT INTO INNTEKTER_I_NORGE DEFAULT VALUES
        """.trimIndent()
        val inntekterINorgeId = connection.executeReturnKey(inntekterINorgeQuery)

        for (entry in arbeidsInntektGrunnlag) {
            for (inntekt in entry.arbeidsInntektInformasjon.inntektListe) {
                val inntektQuery = """
                    INSERT INTO INNTEKT_I_NORGE (identifikator, beloep, skattemessig_bosatt_land, opptjenings_land, inntekt_type, inntekter_i_norge_id, periode) VALUES (?, ?, ?, ?, ?, ?, ?::daterange)
                """.trimIndent()

                val tomFallback = inntekt.opptjeningsperiodeFom ?: entry.aarMaaned.atDay(1)

                connection.execute(inntektQuery) {
                    setParams {
                        setString(1, inntekt.virksomhet.identifikator)
                        setDouble(2, inntekt.beloep)
                        setString(3, inntekt.skattemessigBosattLand)
                        setString(4, inntekt.opptjeningsland)
                        setString(5, inntekt.beskrivelse)
                        setLong(6, inntekterINorgeId)
                        setPeriode(
                            7,
                            Periode(
                                inntekt.opptjeningsperiodeFom ?: entry.aarMaaned.atDay(1),
                                inntekt.opptjeningsperiodeTom ?: tomFallback
                            )
                        )
                    }
                }
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

    private fun hentManuellVurdering(vurderingId: Long?): ManuellVurderingForLovvalgMedlemskap? {
        if (vurderingId == null) return null
        val query = """
            SELECT * FROM LOVVALG_MEDLEMSKAP_MANUELL_VURDERING WHERE ID = ?
        """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {
                ManuellVurderingForLovvalgMedlemskap(
                    lovvalgVedSøknadsTidspunkt = LovvalgVedSøknadsTidspunktDto(
                        begrunnelse = it.getString("tekstvurdering_lovvalg"),
                        lovvalgsEØSLand = it.getEnumOrNull("lovvalgs_land")
                    ),
                    medlemskapVedSøknadsTidspunkt = MedlemskapVedSøknadsTidspunktDto(
                        begrunnelse = it.getStringOrNull("tekstvurdering_medlemskap"),
                        varMedlemIFolketrygd = it.getBooleanOrNull("var_medlem_i_folketrygden")
                    ),
                    overstyrt = it.getBoolean("overstyrt"),
                    vurdertAv = it.getString("vurdert_av"),
                    vurdertDato = it.getLocalDate("opprettet_tid"),
                )
            }
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
        if (arbeiderINorgeId == null) return listOf()

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
        if (inntekterINorgeId == null) return listOf()

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
                    periode = it.getPeriode("periode")
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
                    it.getLongOrNull("manuell_vurdering_id")
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
                (behandling_id, medlemskap_unntak_person_id, inntekter_i_norge_id, arbeider_id, manuell_vurdering_id) 
            SELECT ?, medlemskap_unntak_person_id, inntekter_i_norge_id, arbeider_id, manuell_vurdering_id
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
        val medlemskapUnntakPersonIds = getMedlemskapUnntakPersonIds(behandlingId)
        val inntekterINorgeIds = getInntekterINorgeIds(behandlingId)
        val lovvalgMedlemsskapManuellVurderingIds = getLovvalgMedlemsskapManuellVurderingIds(behandlingId)
        val arbeiderIds = getArbeiderIds(behandlingId)
        val arbeidIds = getArbeidIds(arbeiderIds)
        val inntektINorgeIds = getInntektINorgeIds(inntekterINorgeIds)


        connection.execute("""
            delete from INNTEKT_I_NORGE where inntekter_i_norge_id = ANY(?::bigint[]);     
            delete from ARBEID where arbeider_id = ANY(?::bigint[]);
            delete from ARBEIDER where id = ANY(?::bigint[]);
            delete from MEDLEMSKAP_UNNTAK_PERSON where id = ANY(?::bigint[]);
            delete from MEDLEMSKAP_UNNTAK where medlemskap_unntak_person_id = ANY(?::bigint[]);        
            delete from LOVVALG_MEDLEMSKAP_MANUELL_VURDERING where id = ANY(?::bigint[]);
            delete from OPPGITT_UTENLANDSOPPHOLD_GRUNNLAG where behandling_id = ?; 
            delete from MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG where behandling_id = ?; 
            delete from INNTEKTER_I_NORGE where id = ANY(?::bigint[]);
        """.trimIndent()) {
            setParams {
                setLongArray(1, inntektINorgeIds)
                setLongArray(2, arbeidIds)
                setLongArray(3, arbeiderIds)
                setLongArray(4, medlemskapUnntakPersonIds)
                setLongArray(5, medlemskapUnntakPersonIds)
                setLongArray(6, lovvalgMedlemsskapManuellVurderingIds)
                setLong(7, behandlingId.id)
                setLong(8, behandlingId.id)
                setLongArray(9, inntektINorgeIds)
            }
        }
    }

    private fun getMedlemskapUnntakPersonIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT medlemskap_unntak_person_id
                    FROM MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG
                    WHERE behandling_id = ? AND medlemskap_unntak_person_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("medlemskap_unntak_person_id")
        }
    }

    private fun getLovvalgMedlemsskapManuellVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT manuell_vurdering_id
                    FROM MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG
                    WHERE behandling_id = ? AND manuell_vurdering_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("lovvalg_medlemskap_manuell_vurdering_id")
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
                    SELECT id
                    FROM MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG
                    WHERE arbeider_id = ? 
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("id")
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

    internal data class GrunnlagOppslag(
        val medlId: Long?,
        val inntektINorgeId: Long?,
        val arbeiderId: Long?,
        val manuellVurderingId: Long?
    )

    internal data class InternalHistoriskManuellVurderingForLovvalgMedlemskap(
        val manuellVurdering: ManuellVurderingForLovvalgMedlemskap,
        val vurdertDato: LocalDate
    )
}