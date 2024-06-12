package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository.Beregningsdata.Companion.toBeregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreGrunnlag
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.Year

class BeregningsgrunnlagRepository(private val connection: DBConnection) {

    private class Beregningsdata(
        val beregningsgrunnlagId: Long,
        val beregningId: Long,
        val beregningUføreId: Long,
        val beregningHovedId: Long,
        val beregningYrkesskadeId: Long?,
        val type: GrunnlagUføre.Type,
        val gjeldende: Boolean,
        val gUnitUføre: GUnit,
        val gUnitHoved: GUnit,
        val gUnitYrkesskade: GUnit?
    ) {
        fun parseBeregning(): Beregningsgrunnlag {
            val beregningsgrunnlag = Grunnlag11_19(
                gUnitHoved,
                er6GBegrenset = false,
                erGjennomsnitt = false
            )

            if (beregningYrkesskadeId != null && gUnitYrkesskade != null) {
                return GrunnlagYrkesskade(
                    gUnitYrkesskade,
                    beregningsgrunnlag,
                    terskelverdiForYrkesskade = Prosent(0),
                    andelYrkesskade = Prosent(0),
                    benyttetAndelForYrkesskade = Prosent(0),
                    antattÅrligInntektYrkesskadeTidspunktet = Beløp(0),
                    yrkesskadeTidspunkt = Year.of(0),
                    grunnlagForBeregningAvYrkesskadeandel = GUnit(0),
                    yrkesskadeinntektIG = GUnit(0),
                    andelSomSkyldesYrkesskade = GUnit(0),
                    andelSomIkkeSkyldesYrkesskade = GUnit(0),
                    grunnlagEtterYrkesskadeFordel = GUnit(0),
                    er6GBegrenset = false,
                    erGjennomsnitt = false
                )
            }

            return beregningsgrunnlag
        }

        companion object {
            fun List<Beregningsdata>.toBeregningsgrunnlag(): Beregningsgrunnlag {
                require(this.size in (1..2)) { "Feil antall rader ved henting av beregningsdata: Er ${this.size}" }

                if (this.size == 1) {
                    //Ikke uføre
                    return this.first().parseBeregning()
                }

                val gjeldendeGrunnlag = this.single(Beregningsdata::gjeldende)
                val grunnlag = this.single { it.type == GrunnlagUføre.Type.STANDARD }.parseBeregning()
                val grunnlagYtterligereNedsatt =
                    this.single { it.type == GrunnlagUføre.Type.YTTERLIGERE_NEDSATT }.parseBeregning()

                return GrunnlagUføre(
                    grunnlaget = gjeldendeGrunnlag.gUnitUføre,
                    gjeldende = gjeldendeGrunnlag.type,
                    grunnlag = grunnlag,
                    grunnlagYtterligereNedsatt = grunnlagYtterligereNedsatt,
                    uføregrad = Prosent(0),
                    uføreInntekterFraForegåendeÅr = emptyList(),
                    uføreInntektIKroner = Beløp(0),
                    uføreYtterligereNedsattArbeidsevneÅr = Year.of(0),
                    er6GBegrenset = false,
                    erGjennomsnitt = false
                )
            }
        }
    }

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
                bu.GJELDENDE, 
                bu.G_UNIT AS G_UNIT_UFORE,
                bu.GRUNNLAG_YTTERLIGERE_NEDSATT,
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
                    gjeldende = row.getEnum<GrunnlagUføre.Type>("GJELDENDE"),
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

    fun lagre(behandlingId: BehandlingId, beregningsgrunnlag: Beregningsgrunnlag) {
        val eksisterendeBeregningsgrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeBeregningsgrunnlag == beregningsgrunnlag) return

        if (eksisterendeBeregningsgrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val beregningId = connection.executeReturnKey("INSERT INTO BEREGNING DEFAULT VALUES")

        connection.execute("INSERT INTO BEREGNINGSGRUNNLAG (BEHANDLING_ID, BEREGNING_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, beregningId)
            }
        }
        if (beregningsgrunnlag is GrunnlagUføre) {
            lagreBeregningsgrunnlag(
                beregningId,
                beregningsgrunnlag.underliggende(),
                GrunnlagUføre.Type.STANDARD,
                beregningsgrunnlag.gjeldende() == GrunnlagUføre.Type.STANDARD,
                beregningsgrunnlag.grunnlaget()
            )
            lagreBeregningsgrunnlag(
                beregningId,
                beregningsgrunnlag.underliggendeYtterligereNedsatt(),
                GrunnlagUføre.Type.YTTERLIGERE_NEDSATT,
                beregningsgrunnlag.gjeldende() == GrunnlagUføre.Type.YTTERLIGERE_NEDSATT,
                beregningsgrunnlag.grunnlaget()
            )
            return
        }

        lagreBeregningsgrunnlag(
            beregningId,
            beregningsgrunnlag,
            GrunnlagUføre.Type.STANDARD,
            true,
            beregningsgrunnlag.grunnlaget()
        )
    }

    private fun lagreBeregningsgrunnlag(
        beregningId: Long,
        beregningsgrunnlag: Beregningsgrunnlag,
        type: GrunnlagUføre.Type,
        gjeldende: Boolean,
        grunnlaget: GUnit
    ) {
        val beregningUføreId =
            connection.executeReturnKey("INSERT INTO BEREGNING_UFORE (BEREGNING_ID, TYPE, GJELDENDE, G_UNIT) VALUES (?, ?, ?, ?)") {
                setParams {
                    setLong(1, beregningId)
                    setEnumName(2, type)
                    setBoolean(3, gjeldende)
                    setBigDecimal(4, grunnlaget.verdi())
                }
            }

        when (beregningsgrunnlag) {
            is GrunnlagYrkesskade -> {
                val beregningHovedId = lagreGrunnlag(beregningUføreId, beregningsgrunnlag.underliggende())
                lagreGrunnlagYrkesskade(beregningHovedId, beregningsgrunnlag)
            }

            is Grunnlag11_19 -> {
                lagreGrunnlag(beregningUføreId, beregningsgrunnlag)
            }
        }
    }

    private fun lagreGrunnlag(
        beregningUføreId: Long,
        grunnlag: Beregningsgrunnlag
    ): Long {
        return connection.executeReturnKey("INSERT INTO BEREGNING_HOVED (BEREGNING_UFORE_ID, G_UNIT) VALUES (?, ?)") {
            setParams {
                setLong(1, beregningUføreId)
                setBigDecimal(2, grunnlag.grunnlaget().verdi())
            }
        }
    }

    private fun lagreGrunnlagYrkesskade(beregningHovedId: Long, beregningsgrunnlag: GrunnlagYrkesskade) {
        connection.execute("INSERT INTO BEREGNING_YRKESSKADE (BEREGNING_HOVED_ID, G_UNIT) VALUES (?, ?)") {
            setParams {
                setLong(1, beregningHovedId)
                setBigDecimal(2, beregningsgrunnlag.grunnlaget().verdi())
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
