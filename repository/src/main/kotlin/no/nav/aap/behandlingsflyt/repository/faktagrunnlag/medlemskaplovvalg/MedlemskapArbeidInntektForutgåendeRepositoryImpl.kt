package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdOversikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class MedlemskapArbeidInntektForutgåendeRepositoryImpl(private val connection: DBConnection):
    MedlemskapArbeidInntektForutgåendeRepository {
    companion object : Factory<MedlemskapArbeidInntektForutgåendeRepositoryImpl> {
        override fun konstruer(connection: DBConnection): MedlemskapArbeidInntektForutgåendeRepositoryImpl {
            return MedlemskapArbeidInntektForutgåendeRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): ForutgåendeMedlemskapArbeidInntektGrunnlag? {
        val query = """
            SELECT * FROM FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                ForutgåendeMedlemskapArbeidInntektGrunnlag(
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

    override fun lagreManuellVurdering(behandlingId: BehandlingId, manuellVurdering: ManuellVurderingForForutgåendeMedlemskap){
        val grunnlagOppslag = hentGrunnlag(behandlingId)
        deaktiverGrunnlag(behandlingId)

        val manuellVurderingQuery = """
            INSERT INTO FORUTGAAENDE_MEDLEMSKAP_MANUELL_VURDERING (BEGRUNNELSE, HAR_FORUTGAAENDE_MEDLEMSKAP, VAR_MEDLEM_MED_NEDSATT_ARBEIDSEVNE, MEDLEM_MED_UNNTAK_AV_MAKS_FEM_AAR) VALUES (?, ?, ?, ?)
        """.trimIndent()

        val manuellVurderingId = connection.executeReturnKey(manuellVurderingQuery) {
            setParams {
                setString(1, manuellVurdering.begrunnelse)
                setBoolean(2, manuellVurdering.harForutgåendeMedlemskap)
                setBoolean(3, manuellVurdering.varMedlemMedNedsattArbeidsevne)
                setBoolean(4, manuellVurdering.medlemMedUnntakAvMaksFemAar)
            }
        }

        val grunnlagQuery = """
            INSERT INTO FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG (behandling_id, arbeider_id, inntekter_i_norge_id, medlemskap_unntak_person_id, manuell_vurdering_id) VALUES (?, ?, ?, ?, ?)
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
        arbeidGrunnlag: List<ArbeidsforholdOversikt>,
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
            INSERT INTO FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG (behandling_id, arbeider_id, inntekter_i_norge_id, medlemskap_unntak_person_id, manuell_vurdering_id) VALUES (?, ?, ?, ?, ?)
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

    private fun lagreArbeidGrunnlag (arbeidGrunnlag: List<ArbeidsforholdOversikt>): Long? {
        if (arbeidGrunnlag.isEmpty()) return null

        val arbeiderQuery = """
            INSERT INTO ARBEIDER_FORUTGAAENDE DEFAULT VALUES
        """.trimIndent()
        val arbeiderId = connection.executeReturnKey(arbeiderQuery)

        for (forhold in arbeidGrunnlag) {
            val arbeidQuery = """
                INSERT INTO ARBEID_FORUTGAAENDE (identifikator, arbeidsforhold_kode, arbeider_id, startdato, sluttdato) VALUES (?, ?, ?, ?, ?)
            """.trimIndent()

            connection.execute(arbeidQuery)  {
                setParams {
                    setString(1, forhold.arbeidssted.identer.first().ident)
                    setString(2, forhold.type.kode)
                    setLong(3, arbeiderId)
                    setLocalDate(4, forhold.startdato)
                    setLocalDate(5, forhold.sluttdato)
                }
            }
        }
        return arbeiderId
    }

    private fun lagreArbeidsInntektGrunnlag (arbeidsInntektGrunnlag: List<ArbeidsInntektMaaned>): Long? {
        if (arbeidsInntektGrunnlag.isEmpty()) return null

        val inntekterINorgeQuery = """
            INSERT INTO INNTEKTER_I_NORGE_FORUTGAAENDE DEFAULT VALUES
        """.trimIndent()
        val inntekterINorgeId = connection.executeReturnKey(inntekterINorgeQuery)

        for (entry in arbeidsInntektGrunnlag) {
            for (inntekt in entry.arbeidsInntektInformasjon.inntektListe) {
                val inntektQuery = """
                    INSERT INTO INNTEKT_I_NORGE_FORUTGAAENDE (identifikator, beloep, skattemessig_bosatt_land, opptjenings_land, inntekt_type, inntekter_i_norge_id, periode) VALUES (?, ?, ?, ?, ?, ?, ?::daterange)
                """.trimIndent()

                val tomFallback = inntekt.opptjeningsperiodeFom ?: entry.aarMaaned.atDay(1)

                connection.execute(inntektQuery)  {
                    setParams {
                        setString(1, inntekt.virksomhet.identifikator)
                        setDouble(2, inntekt.beloep)
                        setString(3, inntekt.skattemessigBosattLand)
                        setString(4, inntekt.opptjeningsland)
                        setString(5, inntekt.beskrivelse)
                        setLong(6, inntekterINorgeId)
                        setPeriode(7, Periode(inntekt.opptjeningsperiodeFom?: entry.aarMaaned.atDay(1), inntekt.opptjeningsperiodeTom?: tomFallback))
                    }
                }
            }
        }
        return inntekterINorgeId
    }

    private fun hentManuellVurdering(vurderingId: Long?): ManuellVurderingForForutgåendeMedlemskap?{
        if (vurderingId == null) return null
        val query = """
            SELECT * FROM FORUTGAAENDE_MEDLEMSKAP_MANUELL_VURDERING WHERE ID = ?
        """.trimIndent()

        return connection.queryFirst(query){
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {
                ManuellVurderingForForutgåendeMedlemskap(
                    begrunnelse = it.getString("begrunnelse"),
                    harForutgåendeMedlemskap = it.getBoolean("har_forutgaaende_medlemskap"),
                    varMedlemMedNedsattArbeidsevne = it.getBooleanOrNull("var_medlem_med_nedsatt_arbeidsevne"),
                    medlemMedUnntakAvMaksFemAar = it.getBooleanOrNull("medlem_med_unntak_av_maks_fem_aar")
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

    private fun hentMedlemskapGrunnlag(medlemskapId: Long?): MedlemskapUnntakGrunnlag? {
        if (medlemskapId == null) return null

        val data = connection.queryList(
            """SELECT * FROM MEDLEMSKAP_FORUTGAAENDE_UNNTAK WHERE MEDLEMSKAP_FORUTGAAENDE_UNNTAK_PERSON_ID = ?""".trimIndent()
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
                )
                Segment(
                    it.getPeriode("PERIODE"),
                    unntak
                )
            }
        }.toList()
       return MedlemskapUnntakGrunnlag(data)
    }

    private fun hentArbeiderINorgeGrunnlag(arbeiderINorgeId: Long?): List<ArbeidINorgeGrunnlag> {
        if (arbeiderINorgeId == null) return listOf()

        val query = """
            SELECT * FROM ARBEID_FORUTGAAENDE WHERE arbeider_id = ?
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
            SELECT * FROM INNTEKT_I_NORGE_FORUTGAAENDE WHERE inntekter_i_norge_id = ?
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
            SELECT * FROM FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG WHERE behandling_id = ? and aktiv = true
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
        connection.execute("UPDATE FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    internal data class GrunnlagOppslag(
        val medlId: Long?,
        val inntektINorgeId: Long?,
        val arbeiderId: Long?,
        val manuellVurderingId: Long?
    )
}