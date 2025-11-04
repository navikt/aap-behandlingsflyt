package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Year

class BeregningsgrunnlagRepositoryImpl(private val connection: DBConnection) : BeregningsgrunnlagRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<BeregningsgrunnlagRepositoryImpl> {
        override fun konstruer(connection: DBConnection): BeregningsgrunnlagRepositoryImpl {
            return BeregningsgrunnlagRepositoryImpl(connection)
        }
    }

    enum class Beregningstype {
        STANDARD,
        UFØRE,
        YRKESSKADE,
        YRKESSKADE_UFØRE
    }

    override fun slett(behandlingId: BehandlingId) {

        val beregningIds = getBeregningIds(behandlingId)

        val beregningHovedIds = getBeregningHovedIds(beregningIds)

        val beregningUforeIds = getBeregningUforeIds(beregningIds)

        val beregningInntektIds = getBeregningInntektIds(beregningUforeIds)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from beregning_ufore_tidsperiode where BEREGNING_UFORE_INNTEKT_ID = ANY(?::BIGINT[]);
            delete from beregning_inntekt where beregning_hoved_id = ANY(?::BIGINT[]);
            delete from beregning_ufore_inntekt where beregning_ufore_id = ANY(?::BIGINT[]);
            delete from beregning_ufore where beregning_id = ANY(?::BIGINT[]);
            delete from beregning_hoved where beregning_id = ANY(?::BIGINT[]);
            delete from beregning_yrkesskade where beregning_id = ANY(?::BIGINT[]);
            delete from beregningsgrunnlag where behandling_id = ?;  
            delete from beregning where id = ANY(?::BIGINT[]);
        """.trimIndent()
        ) {
            setParams {
                setLongArray(1, beregningInntektIds)
                setLongArray(2, beregningHovedIds)
                setLongArray(3, beregningUforeIds)
                setLongArray(4, beregningIds)
                setLongArray(5, beregningIds)
                setLongArray(6, beregningIds)
                setLong(7, behandlingId.id)
                setLongArray(8, beregningIds)
            }
        }
        log.info("Slettet $deletedRows rader fra beregning_hoved")
    }

    private fun getBeregningUforeIds(beregningIds: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM beregning_ufore
                    WHERE beregning_id = ANY(?::BIGINT[])
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, beregningIds) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }

    private fun getBeregningHovedIds(beregningIds: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM beregning_hoved
                    WHERE beregning_id = ANY(?::BIGINT[])
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, beregningIds) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }

    private fun getBeregningIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT beregning_id
                    FROM beregningsgrunnlag
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("beregning_id")
        }
    }

    private fun getBeregningInntektIds(beregningIds: List<Long>): List<Long> =
        connection.queryList(
            """
                SELECT id from beregning_ufore_inntekt where beregning_ufore_id = ANY(?::BIGINT[])
            """.trimIndent()
        ) {
            setParams { setLongArray(1, beregningIds) }
            setRowMapper { row ->
                row.getLong("id")
            }
        }

    private fun hentInntekt(beregningsId: Long): List<GrunnlagInntekt> {
        return connection.queryList(
            """
                SELECT BEREGNING_HOVED_ID, ARSTALL, INNTEKT_I_KRONER, GRUNNBELOP, INNTEKT_I_G, INNTEKT_6G_BEGRENSET, ER_6G_BEGRENSET
                FROM BEREGNING_INNTEKT
                WHERE BEREGNING_HOVED_ID = ?
                ORDER BY ARSTALL ASC
            """.trimIndent()
        ) {
            setParams { setLong(1, beregningsId) }
            setRowMapper { row ->
                GrunnlagInntekt(
                    år = Year.of(row.getInt("ARSTALL")),
                    inntektIKroner = Beløp(verdi = row.getBigDecimal("INNTEKT_I_KRONER")),
                    grunnbeløp = Beløp(verdi = row.getBigDecimal("GRUNNBELOP")),
                    inntektIG = GUnit(row.getBigDecimal("INNTEKT_I_G")),
                    inntekt6GBegrenset = GUnit(row.getBigDecimal("INNTEKT_6G_BEGRENSET")),
                    er6GBegrenset = row.getBoolean("ER_6G_BEGRENSET")
                )
            }
        }
    }

    private fun hentStandardBeregning(beregningsId: Long): Grunnlag11_19 {
        return connection.queryFirst(
            """
            SELECT ID, GRUNNLAG, ER_GJENNOMSNITT, GJENNOMSNITTLIG_INNTEKT_I_G
            FROM BEREGNING_HOVED
            WHERE BEREGNING_ID = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, beregningsId) }
            setRowMapper { row ->
                Grunnlag11_19(
                    grunnlaget = GUnit(row.getBigDecimal("GRUNNLAG")),
                    erGjennomsnitt = row.getBoolean("ER_GJENNOMSNITT"),
                    gjennomsnittligInntektIG = GUnit(row.getBigDecimal("GJENNOMSNITTLIG_INNTEKT_I_G")),
                    inntekter = hentInntekt(row.getLong("ID"))
                )
            }
        }
    }

    private fun hentUføreBeregning(beregningsId: Long): GrunnlagUføre {
        val beregningsHoved = connection.queryList(
            """
            SELECT ID, GRUNNLAG, ER_GJENNOMSNITT, GJENNOMSNITTLIG_INNTEKT_I_G
            FROM BEREGNING_HOVED
            WHERE BEREGNING_ID = ?
            """
        ) {
            setParams { setLong(1, beregningsId) }
            setRowMapper { row ->
                val id = row.getLong("ID")
                Pair(
                    id,
                    Grunnlag11_19(
                        grunnlaget = GUnit(row.getBigDecimal("GRUNNLAG")),
                        erGjennomsnitt = row.getBoolean("ER_GJENNOMSNITT"),
                        gjennomsnittligInntektIG = GUnit(row.getBigDecimal("GJENNOMSNITTLIG_INNTEKT_I_G")),
                        inntekter = hentInntekt(id)
                    )
                )
            }
        }

        return connection.queryFirst(
            """
            SELECT ID,
                   TYPE,
                   G_UNIT,
                   BEREGNING_HOVED_ID,
                   BEREGNING_HOVED_YTTERLIGERE_ID,
                   UFOREGRAD,
                   UFORE_YTTERLIGERE_NEDSATT_ARBEIDSEVNE_AR
            FROM BEREGNING_UFORE
            WHERE BEREGNING_ID = ?
            """
        ) {
            setParams { setLong(1, beregningsId) }
            setRowMapper { row ->
                GrunnlagUføre(
                    grunnlaget = GUnit(row.getBigDecimal("G_UNIT")),
                    type = row.getEnum<GrunnlagUføre.Type>("TYPE"),
                    grunnlag = beregningsHoved.first { it.first == row.getLong("BEREGNING_HOVED_ID") }.second,
                    grunnlagYtterligereNedsatt = beregningsHoved.first { it.first == row.getLong("BEREGNING_HOVED_YTTERLIGERE_ID") }.second,
                    uføregrad = Prosent(row.getInt("UFOREGRAD")),
                    uføreInntekterFraForegåendeÅr = hentUføreInntekt(row.getLong("ID")),
                    uføreYtterligereNedsattArbeidsevneÅr = Year.of(row.getInt("UFORE_YTTERLIGERE_NEDSATT_ARBEIDSEVNE_AR"))
                )
            }
        }
    }

    private fun hentUføreInntekt(beregningsId: Long): List<UføreInntekt> {
        return connection.queryList(
            """
                SELECT ID, ARSTALL, INNTEKT_I_KRONER, UFOREGRAD, ARBEIDSGRAD, INNTEKT_JUSTERT_FOR_UFOREGRAD, INNTEKT_I_G, GRUNNBELOP, inntekt_justert_ufore_g
                FROM BEREGNING_UFORE_INNTEKT
                WHERE BEREGNING_UFORE_ID = ?
                ORDER BY ARSTALL ASC
            """.trimIndent()
        ) {
            setParams { setLong(1, beregningsId) }
            setRowMapper { row ->
                UføreInntekt(
                    år = Year.of(row.getInt("ARSTALL")),
                    inntektIKroner = Beløp(verdi = row.getBigDecimal("INNTEKT_I_KRONER")),
                    inntektsPerioder = hentInntektsPerioderUføre(row.getBigDecimal("ID")),
                    inntektJustertForUføregrad = Beløp(row.getInt("INNTEKT_JUSTERT_FOR_UFOREGRAD")),
                    inntektIGJustertForUføregrad = GUnit(row.getInt("inntekt_justert_ufore_g")),
                    inntektIG = GUnit(row.getInt("INNTEKT_I_G")),
                    grunnbeløp = Beløp(row.getInt("grunnbelop")),
                )
            }
        }
    }

    private fun hentInntektsPerioderUføre(id: BigDecimal): List<UføreInntektPeriodisert> {
        return connection.queryList(
            """
                SELECT PERIODE, INNTEKT_I_KRONER, UFOREGRAD, ARBEIDSGRAD, INNTEKT_JUSTERT_FOR_UFOREGRAD, INNTEKT_I_G, GRUNNBELOP, inntekt_justert_ufore_g
                FROM BEREGNING_UFORE_TIDSPERIODE
                WHERE BEREGNING_UFORE_INNTEKT_ID = ?
            """.trimIndent()
        ) {
            setParams { setBigDecimal(1, id) }
            setRowMapper { row ->
                UføreInntektPeriodisert(
                    periode = row.getPeriode("PERIODE"),
                    inntektIKroner = Beløp(row.getInt("INNTEKT_I_KRONER")),
                    inntektIG = GUnit(row.getBigDecimal("INNTEKT_I_G")),
                    uføregrad = Prosent(row.getInt("UFOREGRAD")),
                    arbeidsgrad = Prosent(row.getInt("ARBEIDSGRAD")),
                    inntektJustertForUføregrad = Beløp(row.getInt("INNTEKT_JUSTERT_FOR_UFOREGRAD")),
                    inntektIGJustertForUføregrad = GUnit(row.getInt("inntekt_justert_ufore_g")),
                    grunnbeløp = Beløp(row.getBigDecimal("GRUNNBELOP"))
                )
            }
        }
    }

    private fun hentYrkesskadeBeregning(beregningsId: Long): GrunnlagYrkesskade {
        val beregningsHoved = hentStandardBeregning(beregningsId)

        return hentYrkesskadeBeregning(beregningsId, beregningsHoved)
    }

    private fun hentYrkesskadeBeregning(
        beregningsId: Long,
        beregningsGrunnlag: Beregningsgrunnlag
    ): GrunnlagYrkesskade {

        return connection.queryFirst(
            """
                SELECT by.G_UNIT,
                    by.TERSKELVERDI_FOR_YRKESSKADE,
                    by.ANDEL_YRKESSKADE,
                    by.BENYTTET_ANDEL_YRKESSKADE,
                    by.YRKESSKADE_TIDSPUNKT,
                    by.GRUNNBELOP,
                    by.YRKESSKADE_INNTEKT_I_G,  
                    by.ANTATT_ARLIG_INNTEKT_YRKESSKADE_TIDSPUNKT,
                    by.ANDEL_SOM_SKYLDES_YRKESSKADE,
                    by.ANDEL_SOM_IKKE_SKYLDES_YRKESSKADE,
                    by.GRUNNLAG_ETTER_YRKESSKADE_FORDEL,
                    by.GRUNNLAG_FOR_BEREGNING_AV_YRKESSKADEANDEL
                FROM BEREGNING_YRKESSKADE by
                WHERE by.BEREGNING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, beregningsId)
            }
            setRowMapper { row ->
                GrunnlagYrkesskade(
                    grunnlaget = GUnit(row.getBigDecimal("G_UNIT")),
                    beregningsgrunnlag = beregningsGrunnlag,
                    terskelverdiForYrkesskade = Prosent(row.getInt("TERSKELVERDI_FOR_YRKESSKADE")),
                    andelYrkesskade = Prosent(row.getInt("ANDEL_YRKESSKADE")),
                    benyttetAndelForYrkesskade = Prosent(row.getInt("BENYTTET_ANDEL_YRKESSKADE")),
                    yrkesskadeTidspunkt = Year.of(row.getInt("YRKESSKADE_TIDSPUNKT")),
                    grunnbeløp = Beløp(row.getBigDecimal("GRUNNBELOP")),
                    yrkesskadeinntektIG = GUnit(row.getBigDecimal("YRKESSKADE_INNTEKT_I_G")),
                    antattÅrligInntektYrkesskadeTidspunktet = Beløp(row.getInt("ANTATT_ARLIG_INNTEKT_YRKESSKADE_TIDSPUNKT")),
                    andelSomSkyldesYrkesskade = GUnit(row.getBigDecimal("ANDEL_SOM_SKYLDES_YRKESSKADE")),
                    andelSomIkkeSkyldesYrkesskade = GUnit(row.getBigDecimal("ANDEL_SOM_IKKE_SKYLDES_YRKESSKADE")),
                    grunnlagEtterYrkesskadeFordel = GUnit(row.getBigDecimal("GRUNNLAG_ETTER_YRKESSKADE_FORDEL")),
                    grunnlagForBeregningAvYrkesskadeandel = GUnit(row.getBigDecimal("GRUNNLAG_FOR_BEREGNING_AV_YRKESSKADEANDEL"))
                )
            }
        }
    }

    private fun hentYrkesskadeUføreBeregning(beregningsId: Long): GrunnlagYrkesskade {
        val beregningsHoved = hentUføreBeregning(beregningsId)

        return hentYrkesskadeBeregning(beregningsId, beregningsHoved)
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Beregningsgrunnlag? {

        val beregningsType = connection.queryFirstOrNull(
            """
            SELECT b.BEREGNINGSTYPE, b.ID
            FROM BEREGNINGSGRUNNLAG bg
            INNER JOIN BEREGNING b ON bg.BEREGNING_ID = b.ID
            WHERE bg.AKTIV is true AND bg.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                Pair(
                    row.getLong("ID"),
                    row.getEnum<Beregningstype>("BEREGNINGSTYPE")
                )
            }
        }

        if (beregningsType == null) {
            return null
        }


        return when (beregningsType.second) {
            Beregningstype.STANDARD -> hentStandardBeregning(beregningsType.first)
            Beregningstype.UFØRE -> hentUføreBeregning(beregningsType.first)
            Beregningstype.YRKESSKADE -> hentYrkesskadeBeregning(beregningsType.first)
            Beregningstype.YRKESSKADE_UFØRE -> hentYrkesskadeUføreBeregning(beregningsType.first)
        }

    }

    private fun opprettBeregningId(behandlingId: BehandlingId, beregningstype: Beregningstype): Long {
        val beregningId = connection.executeReturnKey("INSERT INTO BEREGNING (BEREGNINGSTYPE) VALUES (?)") {
            setParams {
                setEnumName(1, beregningstype)
            }
        }

        connection.execute("INSERT INTO BEREGNINGSGRUNNLAG (BEHANDLING_ID, BEREGNING_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, beregningId)
            }
        }

        return beregningId
    }

    private fun lagre(beregningsId: Long, inntekter: List<GrunnlagInntekt>) {
        connection.executeBatch(
            """
            INSERT INTO BEREGNING_INNTEKT
            (BEREGNING_HOVED_ID, ARSTALL, INNTEKT_I_KRONER, GRUNNBELOP, INNTEKT_I_G, INNTEKT_6G_BEGRENSET, ER_6G_BEGRENSET)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
            inntekter
        ) {
            setParams {
                setLong(1, beregningsId)
                setInt(2, it.år.value)
                setBigDecimal(3, it.inntektIKroner.verdi())
                setBigDecimal(4, it.grunnbeløp.verdi())
                setBigDecimal(5, it.inntektIG.verdi())
                setBigDecimal(6, it.inntekt6GBegrenset.verdi())
                setBoolean(7, it.er6GBegrenset)
            }
        }
    }

    private fun lagre(behandlingId: BehandlingId, beregningsgrunnlag: Grunnlag11_19) {
        val beregningstype = Beregningstype.STANDARD
        val beregningsId = opprettBeregningId(behandlingId, beregningstype)

        val key11_19 = lagre(beregningsId, beregningsgrunnlag)
        lagre(key11_19, beregningsgrunnlag.inntekter())
    }

    private fun lagre(beregningsId: Long, beregningsgrunnlag: Grunnlag11_19): Long {
        return connection.executeReturnKey(
            """
            INSERT INTO BEREGNING_HOVED (BEREGNING_ID, GRUNNLAG, ER_GJENNOMSNITT, GJENNOMSNITTLIG_INNTEKT_I_G)
            VALUES (?, ?, ?, ?)"""
        ) {
            setParams {
                setLong(1, beregningsId)
                setBigDecimal(2, beregningsgrunnlag.grunnlaget().verdi())
                setBoolean(3, beregningsgrunnlag.erGjennomsnitt())
                setBigDecimal(4, beregningsgrunnlag.gjennomsnittligInntektIG().verdi())
            }
        }
    }

    private fun lagre(behandlingId: BehandlingId, beregningsgrunnlag: GrunnlagUføre, beregningsIdparam: Long?): Long {
        val beregningstype = Beregningstype.UFØRE
        val beregningsId = beregningsIdparam ?: opprettBeregningId(behandlingId, beregningstype)

        val grunnlagId = lagre(beregningsId, beregningsgrunnlag.underliggende())
        lagre(grunnlagId, beregningsgrunnlag.underliggende().inntekter())

        val ytterligereNedsattId = lagre(beregningsId, beregningsgrunnlag.underliggendeYtterligereNedsatt())
        lagre(ytterligereNedsattId, beregningsgrunnlag.underliggendeYtterligereNedsatt().inntekter())

        val uføreId = connection.executeReturnKey(
            """
INSERT INTO BEREGNING_UFORE (BEREGNING_ID,
                             BEREGNING_HOVED_ID,
                             BEREGNING_HOVED_YTTERLIGERE_ID,
                             TYPE,
                             G_UNIT,
                             UFOREGRAD,
                             UFORE_YTTERLIGERE_NEDSATT_ARBEIDSEVNE_AR)
VALUES (?, ?, ?, ?, ?, ?, ?)"""
        ) {
            setParams {
                setLong(1, beregningsId)
                setLong(2, grunnlagId)
                setLong(3, ytterligereNedsattId)
                setEnumName(4, beregningsgrunnlag.type())
                setBigDecimal(5, beregningsgrunnlag.grunnlaget().verdi())
                setInt(6, beregningsgrunnlag.uføregrad().prosentverdi())
                setInt(7, beregningsgrunnlag.uføreYtterligereNedsattArbeidsevneÅr().value)
            }
        }

        lagreUføreInntekt(uføreId, beregningsgrunnlag.uføreInntekterFraForegåendeÅr())

        return beregningsId
    }

    private fun lagreUføreInntekt(uføreId: Long, inntekter: List<UføreInntekt>) {
        val nyesteUføre = inntekter.flatMap { it.inntektsPerioder }.maxBy { it.periode.fom }
        val ids = inntekter.map {
            connection.executeReturnKey(
                """INSERT INTO BEREGNING_UFORE_INNTEKT
            (BEREGNING_UFORE_ID, ARSTALL, INNTEKT_I_KRONER, UFOREGRAD, ARBEIDSGRAD,
             INNTEKT_JUSTERT_FOR_UFOREGRAD, INNTEKT_I_G, GRUNNBELOP, inntekt_justert_ufore_g)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
            ) {
                // drop not null constraint på uføregrad og arbeidsgrad
                setParams {
                    setLong(1, uføreId)
                    setInt(2, it.år.value)
                    setBigDecimal(3, it.inntektIKroner.verdi())
                    setInt(4, nyesteUføre.uføregrad.prosentverdi())
                    setInt(5, nyesteUføre.uføregrad.komplement().prosentverdi())
                    setBigDecimal(6, it.inntektJustertForUføregrad.verdi())
                    setBigDecimal(7, it.inntektIG.verdi())
                    setBigDecimal(8, it.grunnbeløp.verdi())
                    setBigDecimal(9, it.inntektIGJustertForUføregrad.verdi())
                }
            }
        }

        inntekter.mapIndexed { i, inntekt ->
            lagreUføreInnteksperioder(inntekt.inntektsPerioder, ids[i])
        }

    }

    private fun lagreUføreInnteksperioder(perioder: List<UføreInntektPeriodisert>, beregningUføreInntektID: Long) {
        connection.executeBatch(
            """INSERT INTO BEREGNING_UFORE_TIDSPERIODE(
            BEREGNING_UFORE_INNTEKT_ID, PERIODE, INNTEKT_I_KRONER, INNTEKT_I_G, INNTEKT_JUSTERT_FOR_UFOREGRAD,
            INNTEKT_JUSTERT_UFORE_G, GRUNNBELOP, UFOREGRAD, ARBEIDSGRAD
            ) VALUES (?, ?::daterange, ?, ?, ?, ?, ?, ?, ?)
            """,
            perioder
        ) {
            setParams {
                setLong(1, beregningUføreInntektID)
                setPeriode(2, it.periode)
                setBigDecimal(3, it.inntektIKroner.verdi())
                setBigDecimal(4, it.inntektIG.verdi())
                setBigDecimal(5, it.inntektJustertForUføregrad.verdi())
                setBigDecimal(6, it.inntektIGJustertForUføregrad.verdi())
                setBigDecimal(7, it.grunnbeløp.verdi())
                setInt(8, it.uføregrad.prosentverdi())
                setInt(9, it.arbeidsgrad.prosentverdi())
            }
        }
    }

    private fun lagre(behandlingId: BehandlingId, beregningsgrunnlag: GrunnlagYrkesskade) {
        val beregningstype = Beregningstype.YRKESSKADE
        val beregningsId = opprettBeregningId(behandlingId, beregningstype)

        val underliggendeBeregningsgrunnlag = beregningsgrunnlag.underliggende() as Grunnlag11_19
        val grunnlagId = lagre(beregningsId, underliggendeBeregningsgrunnlag)
        lagre(grunnlagId, underliggendeBeregningsgrunnlag.inntekter())

        connection.execute(
            """INSERT INTO BEREGNING_YRKESSKADE (BEREGNING_ID,
                                    G_UNIT,
                                    TERSKELVERDI_FOR_YRKESSKADE,
                                    ANDEL_YRKESSKADE,
                                    BENYTTET_ANDEL_YRKESSKADE,
                                    YRKESSKADE_TIDSPUNKT,
                                    GRUNNBELOP,
                                    YRKESSKADE_INNTEKT_I_G,
                                    ANTATT_ARLIG_INNTEKT_YRKESSKADE_TIDSPUNKT,
                                    ANDEL_SOM_SKYLDES_YRKESSKADE,
                                    ANDEL_SOM_IKKE_SKYLDES_YRKESSKADE,
                                    GRUNNLAG_ETTER_YRKESSKADE_FORDEL,
                                    GRUNNLAG_FOR_BEREGNING_AV_YRKESSKADEANDEL)
                      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
        ) {
            setParams {
                setLong(1, beregningsId)
                setBigDecimal(2, beregningsgrunnlag.grunnlaget().verdi())
                setInt(3, beregningsgrunnlag.terskelverdiForYrkesskade().prosentverdi())
                setInt(4, beregningsgrunnlag.andelYrkesskade().prosentverdi())
                setInt(5, beregningsgrunnlag.benyttetAndelForYrkesskade().prosentverdi())
                setInt(6, beregningsgrunnlag.yrkesskadeTidspunkt().value)
                setBigDecimal(7, beregningsgrunnlag.grunnbeløp().verdi())
                setBigDecimal(8, beregningsgrunnlag.yrkesskadeinntektIG().verdi())
                setBigDecimal(9, beregningsgrunnlag.antattÅrligInntektYrkesskadeTidspunktet().verdi())
                setBigDecimal(10, beregningsgrunnlag.andelSomSkyldesYrkesskade().verdi())
                setBigDecimal(11, beregningsgrunnlag.andelSomIkkeSkyldesYrkesskade().verdi())
                setBigDecimal(12, beregningsgrunnlag.grunnlagEtterYrkesskadeFordel().verdi())
                setBigDecimal(13, beregningsgrunnlag.grunnlagForBeregningAvYrkesskadeandel().verdi())
            }
        }
    }

    private fun lagreMedUføre(behandlingId: BehandlingId, beregningsgrunnlag: GrunnlagYrkesskade) {
        val beregningstype = Beregningstype.YRKESSKADE_UFØRE
        val beregningId = opprettBeregningId(behandlingId, beregningstype)
        val beregningUføreId = lagre(behandlingId, beregningsgrunnlag.underliggende() as GrunnlagUføre, beregningId)

        connection.execute(
            """INSERT INTO BEREGNING_YRKESSKADE (BEREGNING_ID,
                                                        G_UNIT,
                                                        TERSKELVERDI_FOR_YRKESSKADE,
                                                        ANDEL_YRKESSKADE,
                                                        BENYTTET_ANDEL_YRKESSKADE,
                                                        YRKESSKADE_TIDSPUNKT,
                                                        GRUNNBELOP,
                                                        YRKESSKADE_INNTEKT_I_G,
                                                        ANTATT_ARLIG_INNTEKT_YRKESSKADE_TIDSPUNKT,
                                                        ANDEL_SOM_SKYLDES_YRKESSKADE,
                                                        ANDEL_SOM_IKKE_SKYLDES_YRKESSKADE,
                                                        GRUNNLAG_ETTER_YRKESSKADE_FORDEL,
                                                        GRUNNLAG_FOR_BEREGNING_AV_YRKESSKADEANDEL)
                      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
        ) {
            setParams {
                setLong(1, beregningUføreId)
                setBigDecimal(2, beregningsgrunnlag.grunnlaget().verdi())
                setInt(3, beregningsgrunnlag.terskelverdiForYrkesskade().prosentverdi())
                setInt(4, beregningsgrunnlag.andelYrkesskade().prosentverdi())
                setInt(5, beregningsgrunnlag.benyttetAndelForYrkesskade().prosentverdi())
                setInt(6, beregningsgrunnlag.yrkesskadeTidspunkt().value)
                setBigDecimal(7, beregningsgrunnlag.grunnbeløp().verdi())
                setBigDecimal(8, beregningsgrunnlag.yrkesskadeinntektIG().verdi())
                setBigDecimal(9, beregningsgrunnlag.antattÅrligInntektYrkesskadeTidspunktet().verdi())
                setBigDecimal(10, beregningsgrunnlag.andelSomSkyldesYrkesskade().verdi())
                setBigDecimal(11, beregningsgrunnlag.andelSomIkkeSkyldesYrkesskade().verdi())
                setBigDecimal(12, beregningsgrunnlag.grunnlagEtterYrkesskadeFordel().verdi())
                setBigDecimal(13, beregningsgrunnlag.grunnlagForBeregningAvYrkesskadeandel().verdi())
            }
        }
    }

    override fun lagre(behandlingId: BehandlingId, beregningsgrunnlag: Beregningsgrunnlag) {
        val eksisterendeBeregningsgrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeBeregningsgrunnlag == beregningsgrunnlag) return

        if (eksisterendeBeregningsgrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        when (beregningsgrunnlag) {
            is Grunnlag11_19 -> lagre(behandlingId, beregningsgrunnlag)
            is GrunnlagUføre -> lagre(behandlingId, beregningsgrunnlag, null)
            is GrunnlagYrkesskade -> {
                if (beregningsgrunnlag.underliggende() is GrunnlagUføre) {
                    lagreMedUføre(behandlingId, beregningsgrunnlag)
                } else {
                    lagre(behandlingId, beregningsgrunnlag)
                }
            }
        }
    }


    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE BEREGNINGSGRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
        connection.execute("INSERT INTO BEREGNINGSGRUNNLAG (BEHANDLING_ID, BEREGNING_ID) SELECT ?, BEREGNING_ID FROM BEREGNINGSGRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun deaktiver(behandlingId: BehandlingId) {
        if (hentHvisEksisterer(behandlingId) != null) {
            deaktiverEksisterende(behandlingId)
        }
    }

}

