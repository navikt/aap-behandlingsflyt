package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository.Beregningsdata.Companion.toBeregningsgrunnlag
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

    fun hentHvisEksisterer(behandlingId: BehandlingId): Beregningsgrunnlag? {

        val beregningsdata: List<Beregningsdata> = connection.queryList(
            """
            SELECT  bg.ID AS BEREGNINGSGRUNNLAG_ID,
                    b.ID AS BEREGNING_ID,
                    bu.ID AS BEREGNING_UFORE_ID,
                    bh.ID AS BEREGNING_HOVED_ID,
                    by.ID AS BEREGNING_YRKESSKADE_ID,
                    bu.TYPE,
                    bu.GJELDENDE,
                    bu.G_UNIT AS G_UNIT_UFORE,
                    bh.G_UNIT AS G_UNIT_HOVED,
                    by.G_UNIT AS G_UNIT_YRKESSKADE
            FROM BEREGNINGSGRUNNLAG bg
            INNER JOIN BEREGNING b ON bg.BEREGNING_ID = b.ID
            INNER JOIN BEREGNING_UFORE bu ON b.ID = bu.BEREGNING_ID
            INNER JOIN BEREGNING_HOVED bh ON bu.ID = bh.BEREGNING_UFORE_ID
            LEFT JOIN BEREGNING_YRKESSKADE by ON bh.ID = by.BEREGNING_HOVED_ID
            WHERE bg.AKTIV AND bg.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                val beregningsgrunnlagId = row.getLong("BEREGNINGSGRUNNLAG_ID")
                val beregningId = row.getLong("BEREGNING_ID")
                val beregningUføreId = row.getLong("BEREGNING_UFORE_ID")
                val beregningHovedId = row.getLong("BEREGNING_HOVED_ID")
                val beregningYrkesskadeId = row.getLongOrNull("BEREGNING_YRKESSKADE_ID")
                val type = row.getEnum<GrunnlagUføre.Type>("TYPE")
                val gjeldende = row.getBoolean("GJELDENDE")
                val gUnitUføre = row.getBigDecimal("G_UNIT_UFORE")
                val gUnitHoved = row.getBigDecimal("G_UNIT_HOVED")
                val gUnitYrkesskade = row.getBigDecimalOrNull("G_UNIT_YRKESSKADE")
                Beregningsdata(
                    beregningsgrunnlagId = beregningsgrunnlagId,
                    beregningId = beregningId,
                    beregningUføreId = beregningUføreId,
                    beregningHovedId = beregningHovedId,
                    beregningYrkesskadeId = beregningYrkesskadeId,
                    type = type,
                    gjeldende = gjeldende,
                    gUnitUføre = gUnitUføre.let(::GUnit),
                    gUnitHoved = gUnitHoved.let(::GUnit),
                    gUnitYrkesskade = gUnitYrkesskade?.let(::GUnit)
                )
            }
        }

        val beregningsgrunnlag = beregningsdata
            .groupBy(Beregningsdata::beregningId)
            .mapValues { (_, beregninger) ->
                beregninger.toBeregningsgrunnlag()
            }

        return beregningsgrunnlag.values.firstOrNull()
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
