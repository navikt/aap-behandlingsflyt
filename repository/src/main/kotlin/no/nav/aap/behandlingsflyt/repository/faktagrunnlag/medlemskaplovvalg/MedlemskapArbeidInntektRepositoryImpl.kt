package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdOversikt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMaaned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory

class MedlemskapArbeidInntektRepositoryImpl(private val connection: DBConnection): MedlemskapArbeidInntektRepository {
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
                    arbeiderINorgeGrunnlag = hentArbeiderINorgeGrunnlag(it.getLongOrNull("arbeider_id"))
                )
            }
        }
    }

    override fun lagreArbeidsforholdOgInntektINorge(
        behandlingId: BehandlingId,
        arbeidGrunnlag: List<ArbeidsforholdOversikt>,
        inntektGrunnlag: List<ArbeidsInntektMaaned>,
        medlId: Long?
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val arbeiderQuery = """
            INSERT INTO ARBEIDER DEFAULT VALUES
        """.trimIndent()
        val arbeiderId = connection.executeReturnKey(arbeiderQuery)

        for (forhold in arbeidGrunnlag) {
            val arbeidQuery = """
                INSERT INTO ARBEID (identifikator, arbeidsforhold_kode, arbeider_id, startdato, sluttdato) VALUES (?, ?, ?, ?, ?)
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

        val inntekterINorgeQuery = """
            INSERT INTO INNTEKTER_I_NORGE DEFAULT VALUES
        """.trimIndent()
        val inntekterINorgeId = connection.executeReturnKey(inntekterINorgeQuery)

        for (entry in inntektGrunnlag) {
            for (inntekt in entry.arbeidsInntektInformasjon.inntektListe) {
                val inntektQuery = """
                    INSERT INTO INNTEKT_I_NORGE (identifikator, beloep, skattemessig_bosatt_land, opptjenings_land, inntekt_type, inntekter_i_norge_id, periode) VALUES (?, ?, ?, ?, ?, ?, ?::daterange)
                """.trimIndent()

                val årMånedFallback = if (entry.aarMaaned.atDay(1).isAfter(inntekt.opptjeningsperiodeFom) && inntekt.opptjeningsperiodeFom != null) {
                    entry.aarMaaned.atDay(1)
                } else {
                    inntekt.opptjeningsperiodeFom!!
                }

                connection.execute(inntektQuery)  {
                    setParams {
                        setString(1, inntekt.virksomhet.identifikator)
                        setDouble(2, inntekt.beloep)
                        setString(3, inntekt.skattemessigBosattLand)
                        setString(4, inntekt.opptjeningsland)
                        setString(5, inntekt.beskrivelse)
                        setLong(6, inntekterINorgeId)
                        setPeriode(7, Periode(inntekt.opptjeningsperiodeFom?: årMånedFallback, inntekt.opptjeningsperiodeTom?: årMånedFallback))
                    }
                }
            }
        }

        val grunnlagQuery = """
            INSERT INTO MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG (behandling_id, arbeider_id, inntekter_i_norge_id, medlemskap_unntak_person_id) VALUES (?, ?, ?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, arbeiderId)
                setLong(3, inntekterINorgeId)
                setLong(4, medlId)
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

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    override fun kopier(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        hentHvisEksisterer(fraBehandlingId) ?: return

        val query = """
            INSERT INTO MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG 
                (behandling_id, medlemskap_unntak_person_id, inntekter_i_norge_id, arbeider_id) 
            SELECT ?, medlemskap_unntak_person_id, inntekter_i_norge_id,  arbeider_id
                from MEDLEMSKAP_ARBEID_OG_INNTEKT_I_NORGE_GRUNNLAG 
                where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandlingId.toLong())
                setLong(2, fraBehandlingId.toLong())
            }
        }
    }
}