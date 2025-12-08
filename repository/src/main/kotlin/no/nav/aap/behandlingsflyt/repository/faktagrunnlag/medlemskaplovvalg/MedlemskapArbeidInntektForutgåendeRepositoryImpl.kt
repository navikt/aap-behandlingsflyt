package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemKode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemMedl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class MedlemskapArbeidInntektForutgåendeRepositoryImpl(private val connection: DBConnection) :
    MedlemskapArbeidInntektForutgåendeRepository {

    private val log = LoggerFactory.getLogger(javaClass)

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
                    vurderinger = hentVurderinger(it.getLongOrNull("vurderinger_id"))
                )
            }
        }
    }

    private fun hentVurderinger(vurderingerId: Long?): List<ManuellVurderingForForutgåendeMedlemskap> {
        if (vurderingerId == null) return emptyList()

        val query = """
            SELECT * FROM FORUTGAAENDE_MEDLEMSKAP_MANUELL_VURDERING WHERE VURDERINGER_ID = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper(::mapManuellVurderingForForutgåendeMedlemskap)
        }
    }

    override fun lagreVurderinger(
        behandlingId: BehandlingId,
        vurderinger: List<ManuellVurderingForForutgåendeMedlemskap>
    ) {
        val grunnlagOppslag = hentGrunnlag(behandlingId)
        if (grunnlagOppslag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val vurderingerId = if (vurderinger.isNotEmpty()) lagreVurderinger(vurderinger) else null

        val grunnlagQuery = """
            INSERT INTO FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG (behandling_id, arbeider_id, inntekter_i_norge_id, medlemskap_unntak_person_id, vurderinger_id) VALUES (?, ?, ?, ?, ?)
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

    private fun lagreVurderinger(vurderinger: List<ManuellVurderingForForutgåendeMedlemskap>): Long {
        val overstyrt = vurderinger.any { it.overstyrt }

        val vurderingerId = connection.executeReturnKey(
            """
            INSERT INTO forutgaaende_medlemskap_manuell_vurderinger DEFAULT VALUES
        """.trimIndent()
        )

        val manuellVurderingQuery = """
            INSERT INTO FORUTGAAENDE_MEDLEMSKAP_MANUELL_VURDERING (BEGRUNNELSE, HAR_FORUTGAAENDE_MEDLEMSKAP, 
                VAR_MEDLEM_MED_NEDSATT_ARBEIDSEVNE, MEDLEM_MED_UNNTAK_AV_MAKS_FEM_AAR, OVERSTYRT, VURDERT_AV, 
                OPPRETTET_TID, FOM, TOM, VURDERT_I_BEHANDLING, VURDERINGER_ID) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        connection.executeBatch(manuellVurderingQuery, vurderinger) {
            setParams { manuellVurdering ->
                setString(1, manuellVurdering.begrunnelse)
                setBoolean(2, manuellVurdering.harForutgåendeMedlemskap)
                setBoolean(3, manuellVurdering.varMedlemMedNedsattArbeidsevne)
                setBoolean(4, manuellVurdering.medlemMedUnntakAvMaksFemAar)
                setBoolean(5, overstyrt)
                setString(6, manuellVurdering.vurdertAv)
                setLocalDateTime(7, manuellVurdering.vurdertTidspunkt)
                setLocalDate(8, manuellVurdering.fom)
                setLocalDate(9, manuellVurdering.tom)
                setLong(10, manuellVurdering.vurdertIBehandling?.id)
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
            INSERT INTO FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG (behandling_id, arbeider_id, inntekter_i_norge_id, medlemskap_unntak_person_id, vurderinger_id) VALUES (?, ?, ?, ?, ?)
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

    override fun slett(behandlingId: BehandlingId) {
        val arbeidIds = getArbeidIds(behandlingId)
        val inntekterIds = getInntekterIds(behandlingId)
        val vurderingerIds = getManuellVurderingerIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG where behandling_id = ?; 
            delete from INNTEKT_I_NORGE_FORUTGAAENDE where id = ANY(?::bigint[]);
            delete from ARBEID_FORUTGAAENDE where arbeider_id = ANY(?::bigint[]);
            delete from ARBEIDER_FORUTGAAENDE where id = ANY(?::bigint[]);
            delete from FORUTGAAENDE_MEDLEMSKAP_MANUELL_VURDERING where vurderinger_id = ANY(?::bigint[]);
            delete from FORUTGAAENDE_MEDLEMSKAP_MANUELL_VURDERINGER where id = ANY(?::bigint[]);
           
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, inntekterIds)
                setLongArray(3, arbeidIds)
                setLongArray(4, arbeidIds)
                setLongArray(5, vurderingerIds)
                setLongArray(6, vurderingerIds)
            }
        }
        log.info("Slettet $deletedRows rader fra FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG")
    }

    private fun getManuellVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT vurderinger_id
                    FROM FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG
                    WHERE behandling_id = ? AND vurderinger_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurderinger_id")
        }
    }

    private fun getArbeidIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT arbeider_id
                    FROM FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG
                    WHERE behandling_id = ? AND arbeider_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("arbeider_id")
        }
    }

    private fun getInntekterIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT inntekter_i_norge_id
                    FROM FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG
                    WHERE behandling_id = ? AND inntekter_i_norge_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("inntekter_i_norge_id")
        }
    }

    private fun lagreArbeidGrunnlag(arbeidGrunnlag: List<ArbeidINorgeGrunnlag>): Long? {
        if (arbeidGrunnlag.isEmpty()) return null

        val arbeiderQuery = """
            INSERT INTO ARBEIDER_FORUTGAAENDE DEFAULT VALUES
        """.trimIndent()
        val arbeiderId = connection.executeReturnKey(arbeiderQuery)

        for (forhold in arbeidGrunnlag) {
            val arbeidQuery = """
                INSERT INTO ARBEID_FORUTGAAENDE (identifikator, arbeidsforhold_kode, arbeider_id, startdato, sluttdato) VALUES (?, ?, ?, ?, ?)
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
            INSERT INTO INNTEKTER_I_NORGE_FORUTGAAENDE DEFAULT VALUES
        """.trimIndent()
        val inntekterINorgeId = connection.executeReturnKey(inntekterINorgeQuery)

        val inntektQuery = """
                    INSERT INTO INNTEKT_I_NORGE_FORUTGAAENDE (identifikator, beloep, skattemessig_bosatt_land, opptjenings_land, inntekt_type, inntekter_i_norge_id, periode, organisasjonsnavn) VALUES (?, ?, ?, ?, ?, ?, ?::daterange, ?)
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
                val orgNavn =
                    enhetGrunnlag.firstOrNull { it.orgnummer == inntekt.virksomhet.identifikator }?.orgNavn
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

    private fun mapManuellVurderingForForutgåendeMedlemskap(row: Row): ManuellVurderingForForutgåendeMedlemskap =
        ManuellVurderingForForutgåendeMedlemskap(
            begrunnelse = row.getString("begrunnelse"),
            harForutgåendeMedlemskap = row.getBoolean("har_forutgaaende_medlemskap"),
            varMedlemMedNedsattArbeidsevne = row.getBooleanOrNull("var_medlem_med_nedsatt_arbeidsevne"),
            medlemMedUnntakAvMaksFemAar = row.getBooleanOrNull("medlem_med_unntak_av_maks_fem_aar"),
            overstyrt = row.getBoolean("overstyrt"),
            vurdertAv = row.getString("vurdert_av"),
            vurdertTidspunkt = row.getLocalDateTime("opprettet_tid"),
            vurdertIBehandling = row.getLong("vurdert_i_behandling").let { id -> BehandlingId(id) },
            fom = row.getLocalDate("fom"),
            tom = row.getLocalDateOrNull("tom"),
        )

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
        if (inntekterINorgeId == null) return emptyList()

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
                    periode = it.getPeriode("periode"),
                    organisasjonsNavn = it.getStringOrNull("organisasjonsnavn")
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
                    it.getLongOrNull("vurderinger_id")
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

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        hentHvisEksisterer(fraBehandling) ?: return

        val query = """
            INSERT INTO FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG 
                (behandling_id, medlemskap_unntak_person_id, inntekter_i_norge_id, arbeider_id, vurderinger_id) 
            SELECT ?, medlemskap_unntak_person_id, inntekter_i_norge_id, arbeider_id, vurderinger_id
                from FORUTGAAENDE_MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG 
                where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    internal data class GrunnlagOppslag(
        val medlId: Long?,
        val inntektINorgeId: Long?,
        val arbeiderId: Long?,
        val vurderingerId: Long?
    )
}