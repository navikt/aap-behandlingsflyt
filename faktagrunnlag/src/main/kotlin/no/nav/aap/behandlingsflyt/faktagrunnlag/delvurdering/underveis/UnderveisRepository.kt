package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.Dagsatser
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.TimerArbeid
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class UnderveisRepository(private val connection: DBConnection) {

    fun hent(behandlingId: BehandlingId): UnderveisGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): UnderveisGrunnlag? {
        val query = """
            SELECT * FROM UNDERVEIS_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                mapGrunnlag(it)
            }
        }
    }

    private fun mapGrunnlag(row: Row): UnderveisGrunnlag {
        val pliktkorteneId = row.getLong("perioder_id")

        val query = """
            SELECT * FROM UNDERVEIS_PERIODE WHERE perioder_id = ?
        """.trimIndent()

        val underveisperioder = connection.queryList(query) {
            setParams {
                setLong(1, pliktkorteneId)
            }
            setRowMapper {
                mapPeriode(it)
            }
        }.toList()

        return UnderveisGrunnlag(row.getLong("id"), underveisperioder)
    }

    private fun mapPeriode(it: Row): Underveisperiode {

        val antallTimer = it.getBigDecimalOrNull("timer_arbeid")
        val graderingProsent = it.getIntOrNull("gradering")
        val andelArbeidsevne = it.getIntOrNull("andel_arbeidsevne")

        val gradering = if (antallTimer == null || graderingProsent == null || andelArbeidsevne == null) {
            null
        } else {
            val gradering = Prosent(graderingProsent)
            Gradering(
                totaltAntallTimer = TimerArbeid(antallTimer),
                andelArbeid = Prosent.`100_PROSENT`.minus(gradering),
                fastsattArbeidsevne = Prosent(andelArbeidsevne),
                gradering = gradering,
            )
        }

        return Underveisperiode(
            it.getPeriode("periode"),
            it.getPeriodeOrNull("meldeperiode"),
            it.getEnum("utfall"),
            it.getEnumOrNull("avslagsarsak"),
            Prosent(it.getInt("grenseverdi")),
            gradering,
            Dagsatser(it.getInt("trekk_dagsatser"))
        )
    }


    fun lagre(
        behandlingId: BehandlingId,
        underveisperioder: List<Underveisperiode>,
        input: Faktagrunnlag
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val eksisterendePerioder = eksisterendeGrunnlag?.perioder ?: emptySet()

        if (eksisterendePerioder != underveisperioder) {
            if (eksisterendeGrunnlag != null) {
                deaktiverGrunnlag(behandlingId)
            }

            lagreNyttGrunnlag(behandlingId, underveisperioder, input)
        }
    }

    private fun lagreNyttGrunnlag(
        behandlingId: BehandlingId,
        underveisperioder: List<Underveisperiode>,
        input: Faktagrunnlag
    ) {
        val pliktkorteneQuery = """
            INSERT INTO UNDERVEIS_PERIODER DEFAULT VALUES
            """.trimIndent()
        val perioderId = connection.executeReturnKey(pliktkorteneQuery)

        val query = """
            INSERT INTO UNDERVEIS_PERIODE (perioder_id, periode, utfall, avslagsarsak, grenseverdi, timer_arbeid, gradering, meldeperiode, trekk_dagsatser, andel_arbeidsevne) VALUES (?, ?::daterange, ?, ?, ?, ?, ?, ?::daterange, ?, ?)
            """.trimIndent()
        connection.executeBatch(query, underveisperioder) {
            setParams { periode ->
                setLong(1, perioderId)
                setPeriode(2, periode.periode)
                setEnumName(3, periode.utfall)
                setEnumName(4, periode.avslagsårsak)
                setInt(5, periode.grenseverdi.prosentverdi())
                setBigDecimal(6, periode.gradering?.totaltAntallTimer?.antallTimer)
                setInt(7, periode.gradering?.gradering?.prosentverdi())
                setPeriode(8, periode.meldePeriode)
                setInt(9, periode.trekk.antall)
                setInt(10, periode.gradering?.fastsattArbeidsevne?.prosentverdi())
            }
        }

        val sporingQuery = """
            INSERT INTO UNDERVEIS_SPORING (FAKTAGRUNNLAG, VERSJON) VALUES (?, ?)
            """.trimIndent()
        val sporingId = connection.executeReturnKey(sporingQuery) {
            setParams {
                setString(1, input.hent())
                setString(2, ApplikasjonsVersjon.versjon)
            }
        }

        val grunnlagQuery = """
            INSERT INTO UNDERVEIS_GRUNNLAG (behandling_id, perioder_id, sporing_id) VALUES (?, ?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, perioderId)
                setLong(3, sporingId)
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE UNDERVEIS_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    fun kopier(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandlingId)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO UNDERVEIS_GRUNNLAG (behandling_id, perioder_id, sporing_id) SELECT ?, perioder_id, sporing_id from UNDERVEIS_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandlingId.toLong())
                setLong(2, fraBehandlingId.toLong())
            }
        }
    }
}