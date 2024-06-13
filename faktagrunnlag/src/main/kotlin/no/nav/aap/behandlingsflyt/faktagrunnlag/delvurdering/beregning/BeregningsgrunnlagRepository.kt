package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreGrunnlag
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.Year

class BeregningsgrunnlagRepository(private val connection: DBConnection) {



    enum class Beregningstype{
        STANDARD,
        UFØRE,
        YRKESSKADE,
        YRKESSKADE_UFØRE
    }
    fun hentStandardBeregning(beregningsId:Long):Grunnlag11_19{
        return connection.queryFirst<Grunnlag11_19>(
            """
            SELECT  bh.G_UNIT AS G_UNIT_HOVED
                    FROM BEREGNING_HOVED bh 
                    WHERE bh.BEREGNING_ID = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, beregningsId) }
            setRowMapper { row ->
                Grunnlag11_19(GUnit(row.getBigDecimal("G_UNIT_HOVED")),false, false) //TODO:tulledata
            }
        }
    }
    fun hentUføreBeregning(beregningsId:Long): GrunnlagUføre{
        val beregningsHoved = connection.queryList(
            """
            SELECT  bh.G_UNIT AS G_UNIT_HOVED,
                    bh.ID
                    FROM BEREGNING_HOVED bh 
                    WHERE bh.BEREGNING_ID = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, beregningsId) }
            setRowMapper { row ->
                Pair(row.getLong("ID"),Grunnlag11_19(GUnit(row.getBigDecimal("G_UNIT_HOVED")),false, false)) //TODO:tulledata
            }
        }

        return connection.queryFirst(
            """
            SELECT  bu.TYPE, 
                bu.G_UNIT AS G_UNIT_UFORE,
                bu.BEREGNING_HOVED_ID,
                bu.BEREGNING_HOVED_YTTERLIGERE_ID,
                bu.UFOREGRAD,
                UFORE_YTTERLIG_NEDSATT_ARBEIDSEVNE_AR
                FROM BEREGNING_UFORE bu 
                    WHERE bu.BEREGNING_ID = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, beregningsId) }
            setRowMapper { row ->
                GrunnlagUføre(
                    grunnlaget = GUnit(row.getBigDecimal("G_UNIT")),
                    type = row.getEnum<GrunnlagUføre.Type>("TYPE"),
                    grunnlag = beregningsHoved.first { it.first == row.getLong("BEREGNING_HOVED_ID") }.second,
                    grunnlagYtterligereNedsatt = beregningsHoved.first { it.first == row.getLong("BEREGNING_HOVED_YTTERLIGERE_ID") }.second,
                    uføregrad = Prosent(row.getInt("UFOREGRAD")),
                    uføreInntekterFraForegåendeÅr = emptyList(), //TODO: egen henting for inntekt
                    uføreInntektIKroner = Beløp(0), //TODO: egen henting for inntekt
                    uføreYtterligereNedsattArbeidsevneÅr = Year.of(row.getInt("UFORE_YTTERLIG_NEDSATT_ARBEIDSEVNE_AR")),
                    er6GBegrenset = false, //TODO: egen henting for inntekt
                    erGjennomsnitt = false //TODO: egen henting for inntekt
                    )
            }
        }

    }
    fun hentYrkesskadeBeregning(beregningsId:Long): GrunnlagYrkesskade {
        val beregningsHoved = hentStandardBeregning(beregningsId)

        return hentYrkesskadeBeregning(beregningsId, beregningsHoved)
    }
    private fun hentYrkesskadeBeregning(beregningsId:Long, beregningsGrunnlag:Beregningsgrunnlag): GrunnlagYrkesskade{

        return connection.queryFirst(
            """
                SELECT by.G_UNIT,
                    by.TERSKELVERDI_FOR_YRKESKADE,
                    by.ANDEL_YRKESKADE,
                    by.BENYTTET_ANDEL_FOR_YRKESKADE,
                    by.YRKESSKADE_TIDSPUNKT,
                    by.YRKESSKADE_INNTEKT_I_G,  
                    by.ANTATT_ARLIG_INNTEKT_YRKESKADE_TIDSPUNKTET,
                    by.ANDEL_SOM_SKYLDES_YRKESKADE,
                    by.ANDEL_SOM_IKKE_SKYLDES_YRKESKADE,
                    by.GRUNNLAG_ETTER_YRKESKADE_FORDEL,
                    by.GRUNNLAG_FOR_BEREGNING_AV_YRKESKADEANDEL,
                    by.ER_6G_BEGRENSET,
                    by.ER_GJENNOMSNITT
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
                    terskelverdiForYrkesskade = Prosent(row.getInt("TERSKEVERDI_FOR_YRKESKADE")),
                    andelYrkesskade = Prosent(row.getInt("ANDEL_YRKESKADE")),
                    benyttetAndelForYrkesskade = Prosent(row.getInt("BENYTTET_ANDEL_FOR_YRKESKADE")),
                    yrkesskadeTidspunkt = Year.of(row.getInt("YRKESSKADE_TIDSPUNKT")),
                    yrkesskadeinntektIG = GUnit(row.getBigDecimal("YRKESSKADE_INNTEKT_I_G")),
                    antattÅrligInntektYrkesskadeTidspunktet = Beløp(row.getInt("ANTATT_ARLIG_INNTEKT_YRKESKADE_TIDSPUNKTET")),
                    andelSomSkyldesYrkesskade = GUnit(row.getBigDecimal("ANDEL_SOM_SKYLDES_YRKESKADE")),
                    andelSomIkkeSkyldesYrkesskade = GUnit(row.getBigDecimal("ANDEL_SOM_IKKE_SKYLDES_YRKESKADE")),
                    grunnlagEtterYrkesskadeFordel = GUnit(row.getBigDecimal("GRUNNLAG_ETTER_YRKESKADE_FORDEL")),
                    grunnlagForBeregningAvYrkesskadeandel = GUnit(row.getBigDecimal("GRUNNLAG_FOR_BEREGNING_AV_YRKESKADEANDEL")),
                    er6GBegrenset = row.getBoolean("ER_6G_BEGRENSET"),
                    erGjennomsnitt = row.getBoolean("ER_GJENNOMSNITT")
                    )
            }
        }
    }
    fun hentYrkesskadeUføreBeregning(beregningsId:Long): GrunnlagYrkesskade{
        val beregningsHoved = hentUføreBeregning(beregningsId)

        return hentYrkesskadeBeregning(beregningsId, beregningsHoved)
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): Beregningsgrunnlag? {

        val beregningsType = connection.queryFirstOrNull(
            """
            SELECT  b.BEREGNINGSTYPE, b.ID
            FROM BEREGNINGSGRUNNLAG bg
            INNER JOIN BEREGNING b ON bg.BEREGNING_ID = b.ID
            WHERE bg.AKTIV AND bg.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                row.getLong("ID") to
                        row.getEnum<Beregningstype>("BEREGNINGSTYPE")
            }
        }

        if (beregningsType == null) return null


        return when(beregningsType.second){
            Beregningstype.STANDARD -> hentStandardBeregning(beregningsType.first)
            Beregningstype.UFØRE -> hentUføreBeregning(beregningsType.first)
            Beregningstype.YRKESSKADE -> hentYrkesskadeBeregning(beregningsType.first)
            Beregningstype.YRKESSKADE_UFØRE -> hentYrkesskadeUføreBeregning(beregningsType.first)
        }

    }

    fun opprettBeregningId(behandlingId: BehandlingId, beregningstype: Beregningstype):Long{
        val beregningId = connection.executeReturnKey("INSERT INTO BEREGNING (BEREGNINGSTYPE) VALUES (?)"){
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
    
    private fun lagre(behandlingId: BehandlingId, beregningsgrunnlag: Grunnlag11_19){
        val beregningstype = Beregningstype.STANDARD
        val beregningsId = opprettBeregningId(behandlingId, beregningstype)

        lagre(beregningsId, beregningsgrunnlag)
    }

    private fun lagre(beregningsId: Long, beregningsgrunnlag: Grunnlag11_19): Long{
        return connection.executeReturnKey("INSERT INTO BEREGNING_HOVED (BEREGNING_ID, G_UNIT) VALUES (?, ?)") {
            setParams {
                setLong(1, beregningsId)
                setBigDecimal(2, beregningsgrunnlag.grunnlaget().verdi())
            }
        }
    }

    private fun lagre(behandlingId: BehandlingId, beregningsgrunnlag: GrunnlagUføre, beregningsIdparam: Long?):Long{
        val beregningstype = Beregningstype.UFØRE
        val beregningsId = beregningsIdparam?: opprettBeregningId(behandlingId, beregningstype)

        val grunnlagId = lagre(beregningsId, beregningsgrunnlag.underliggende())
        val ytterligereNedsattId = lagre(beregningsId, beregningsgrunnlag.underliggendeYtterligereNedsatt())

        connection.executeReturnKey(
            """
                INSERT INTO BEREGNING_UFORE (
                BEREGNING_ID, 
                BEREGNING_HOVED_ID,
                BEREGNING_HOVED_YTTERLIGERE_ID,
                G_UNIT,
                UFOREGRAD,
                UFORE_YTTERLIG_NEDSATT_ARBEIDSEVNE_AR
                )VALUES (?, ?, ?, ?, ?, ?)""".trimIndent()
        ) {
            setParams {
                setLong(1, beregningsId)
                setLong(2, grunnlagId)
                setLong(3, ytterligereNedsattId)
                setBigDecimal(4, beregningsgrunnlag.grunnlaget().verdi())
                setInt(5, beregningsgrunnlag.uføregrad().prosentverdi())
                setInt(6, beregningsgrunnlag.uføreYtterligereNedsattArbeidsevneÅr().value)
            }
        }
        return beregningsId
    }

    private fun lagre(behandlingId: BehandlingId, beregningsgrunnlag: GrunnlagYrkesskade){
        val beregningstype = Beregningstype.YRKESSKADE
        val beregningsId = opprettBeregningId(behandlingId, beregningstype)

        val grunnlagId = lagre(beregningsId, beregningsgrunnlag.underliggende() as Grunnlag11_19)

        connection.execute("""
            INSERT INTO BEREGNING_YRKESSKADE (
            BEREGNING_ID, 
            G_UNIT,
            TERSKELVERDI_FOR_YRKESKADE,
            ANDEL_YRKESKADE,
            BENYTTET_ANDEL_FOR_YRKESKADE,
            YRKESSKADE_TIDSPUNKT,
            YRKESSKADE_INNTEKT_I_G,
            ANTATT_ARLIG_INNTEKT_YRKESKADE_TIDSPUNKTET,
            ANDEL_SOM_SKYLDES_YRKESKADE,
            ANDEL_SOM_IKKE_SKYLDES_YRKESKADE,
            GRUNNLAG_ETTER_YRKESKADE_FORDEL,
            GRUNNLAG_FOR_BEREGNING_AV_YRKESKADEANDEL,
            ER_6G_BEGRENSET,
            ER_GJENNOMSNITT
            )VALUES (?, ?, ?,?,?,?,?,?,?,?,?,?,?,?)""".trimIndent()) {
            setParams {
                setLong(1, grunnlagId)
                setBigDecimal(2, beregningsgrunnlag.grunnlaget().verdi())
                setInt(3, beregningsgrunnlag.terskelverdiForYrkesskade().prosentverdi())
                setInt(4, beregningsgrunnlag.andelYrkesskade().prosentverdi())
                setInt(5, beregningsgrunnlag.benyttetAndelForYrkesskade().prosentverdi())
                setInt(6, beregningsgrunnlag.yrkesskadeTidspunkt().value)
                setBigDecimal(7, beregningsgrunnlag.yrkesskadeinntektIG().verdi())
                setBigDecimal(8, beregningsgrunnlag.antattÅrligInntektYrkesskadeTidspunktet().verdi())
                setBigDecimal(9, beregningsgrunnlag.andelSomSkyldesYrkesskade().verdi())
                setBigDecimal(10, beregningsgrunnlag.andelSomIkkeSkyldesYrkesskade().verdi())
                setBigDecimal(11, beregningsgrunnlag.grunnlagEtterYrkesskadeFordel().verdi())
                setBigDecimal(12, beregningsgrunnlag.grunnlagForBeregningAvYrkesskadeandel().verdi())
                setBoolean(13, beregningsgrunnlag.er6GBegrenset())
                setBoolean(14, beregningsgrunnlag.erGjennomsnitt())

            }

        }

    }
    private fun lagreMedUføre(behandlingId: BehandlingId, beregningsgrunnlag: GrunnlagYrkesskade){
        val beregningstype = Beregningstype.YRKESSKADE_UFØRE
        val beregningId = opprettBeregningId(behandlingId, beregningstype)
        val beregningUføreId = lagre(behandlingId, beregningsgrunnlag.underliggende() as GrunnlagUføre, beregningId)


        connection.execute("""
            INSERT INTO BEREGNING_YRKESSKADE (
            BEREGNING_ID,
            G_UNIT,
            TERSKELVERDI_FOR_YRKESKADE,
            ANDEL_YRKESKADE,
            BENYTTET_ANDEL_FOR_YRKESKADE,
            YRKESSKADE_TIDSPUNKT,
            YRKESSKADE_INNTEKT_I_G,
            ANTATT_ARLIG_INNTEKT_YRKESKADE_TIDSPUNKTET,
            ANDEL_SOM_SKYLDES_YRKESKADE,
            ANDEL_SOM_IKKE_SKYLDES_YRKESKADE,
            GRUNNLAG_ETTER_YRKESKADE_FORDEL,
            GRUNNLAG_FOR_BEREGNING_AV_YRKESKADEANDEL,
            ER_6G_BEGRENSET,
            ER_GJENNOMSNITT
            )VALUES (?, ?, ?,?,?,?,?,?,?,?,?,?,?,?)""".trimIndent()) {
            setParams {
                setLong(1, beregningUføreId)
                setBigDecimal(2, beregningsgrunnlag.grunnlaget().verdi())
                setInt(3, beregningsgrunnlag.terskelverdiForYrkesskade().prosentverdi())
                setInt(4, beregningsgrunnlag.andelYrkesskade().prosentverdi())
                setInt(5, beregningsgrunnlag.benyttetAndelForYrkesskade().prosentverdi())
                setInt(6, beregningsgrunnlag.yrkesskadeTidspunkt().value)
                setBigDecimal(7, beregningsgrunnlag.yrkesskadeinntektIG().verdi())
                setBigDecimal(8, beregningsgrunnlag.antattÅrligInntektYrkesskadeTidspunktet().verdi())
                setBigDecimal(9, beregningsgrunnlag.andelSomSkyldesYrkesskade().verdi())
                setBigDecimal(10, beregningsgrunnlag.andelSomIkkeSkyldesYrkesskade().verdi())
                setBigDecimal(11, beregningsgrunnlag.grunnlagEtterYrkesskadeFordel().verdi())
                setBigDecimal(12, beregningsgrunnlag.grunnlagForBeregningAvYrkesskadeandel().verdi())
                setBoolean(13, beregningsgrunnlag.er6GBegrenset())
                setBoolean(14, beregningsgrunnlag.erGjennomsnitt())
            }
        }
    }

    fun lagre(behandlingId: BehandlingId, beregningsgrunnlag: Beregningsgrunnlag) {
        val eksisterendeBeregningsgrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeBeregningsgrunnlag == beregningsgrunnlag) return

        if (eksisterendeBeregningsgrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        when(beregningsgrunnlag){
            is Grunnlag11_19 -> lagre(behandlingId, beregningsgrunnlag)
            is GrunnlagUføre -> lagre(behandlingId, beregningsgrunnlag, null)
            is GrunnlagYrkesskade -> {
                if(beregningsgrunnlag.underliggende() is GrunnlagUføre){
                    lagreMedUføre(behandlingId, beregningsgrunnlag)
                }
                else lagre(behandlingId, beregningsgrunnlag)
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

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute("INSERT INTO BEREGNINGSGRUNNLAG (BEHANDLING_ID, BEREGNING_ID) SELECT ?, BEREGNING_ID FROM BEREGNINGSGRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
