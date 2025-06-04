package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisperiodeId
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class UnderveisRepositoryImpl(private val connection: DBConnection) : UnderveisRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<UnderveisRepositoryImpl> {
        override fun konstruer(connection: DBConnection): UnderveisRepositoryImpl {
            return UnderveisRepositoryImpl(connection)
        }
    }

    override fun hent(behandlingId: BehandlingId): UnderveisGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId)) { "Fant ikke underveisgrunnlag for behandlingId=$behandlingId" }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): UnderveisGrunnlag? {
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
        val meldekorteneId = row.getLong("perioder_id")

        val query = """
            SELECT * FROM UNDERVEIS_PERIODE WHERE perioder_id = ? ORDER BY periode
        """.trimIndent()

        val underveisperioder = connection.queryList(query) {
            setParams {
                setLong(1, meldekorteneId)
            }
            setRowMapper {
                mapPeriode(it)
            }
        }.toList()

        return UnderveisGrunnlag(row.getLong("id"), underveisperioder)
    }

    fun hentPerioder(ider: List<UnderveisperiodeId>): List<Underveisperiode> {
        return connection.queryList("""select * from underveis_periode where id = any(?::bigint[])""") {
            setParams {
                setLongArray(1, ider.map { it.asLong })
            }
            setRowMapper { row -> mapPeriode(row) }
        }
    }


    private fun mapPeriode(it: Row): Underveisperiode {

        val antallTimer = it.getBigDecimal("timer_arbeid")
        val graderingProsent = it.getInt("gradering")
        val andelArbeidsevne = it.getInt("andel_arbeidsevne")

        val arbeidsGradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(antallTimer),
            andelArbeid = Prosent.`100_PROSENT`.minus(Prosent(graderingProsent)),
            fastsattArbeidsevne = Prosent(andelArbeidsevne),
            gradering = Prosent(graderingProsent),
            opplysningerMottatt = it.getLocalDateOrNull("meldekort_mottatt"),
        )

        return Underveisperiode(
            periode = it.getPeriode("periode"),
            meldePeriode = it.getPeriode("meldeperiode"),
            utfall = it.getEnum("utfall"),
            rettighetsType = it.getEnumOrNull("rettighetstype"),
            avslagsårsak = it.getEnumOrNull("avslagsarsak"),
            grenseverdi = Prosent(it.getInt("grenseverdi")),
            arbeidsgradering = arbeidsGradering,
            trekk = Dagsatser(it.getInt("trekk_dagsatser")),
            brukerAvKvoter = it.getArray("bruker_av_kvoter", String::class).map { Kvote.valueOf(it) }.toSet(),
            bruddAktivitetspliktId = it.getLongOrNull("brudd_aktivitetsplikt_id")?.let { BruddAktivitetspliktId(it) },
            id = UnderveisperiodeId(it.getLong("id")),
            institusjonsoppholdReduksjon = Prosent(it.getInt("institusjonsoppholdreduksjon")),
            meldepliktStatus = it.getEnumOrNull("meldeplikt_status"),
        )
    }


    override fun lagre(
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

    override fun slett(behandlingId: BehandlingId) {

        val sporingIds = getSporingIds(behandlingId)
        val periodeIds = getPerioderIds(behandlingId)
        val deletedRows = connection.executeReturnUpdated("""
            delete from underveis_grunnlag where behandling_id = ?; 
            delete from underveis_periode where perioder_id = ANY(?::bigint[]);
            delete from underveis_perioder where id = ANY(?::bigint[]);
            delete from underveis_sporing where id = ANY(?::bigint[]);
          
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, periodeIds)
                setLongArray(3, periodeIds)
                setLongArray(4, sporingIds)
            }
        }
        log.info("Slettet $deletedRows rader fra underveis_grunnlag")
    }

    private fun getSporingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT sporing_id
                    FROM underveis_grunnlag
                    WHERE behandling_id = ? AND sporing_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("sporing_id")
        }
    }

    private fun getPerioderIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT perioder_id
                    FROM underveis_grunnlag
                    WHERE behandling_id = ? AND perioder_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("perioder_id")
        }
    }

    private fun lagreNyttGrunnlag(
        behandlingId: BehandlingId,
        underveisperioder: List<Underveisperiode>,
        input: Faktagrunnlag
    ) {
        val meldekorteneQuery = """
            INSERT INTO UNDERVEIS_PERIODER DEFAULT VALUES
            """.trimIndent()
        val perioderId = connection.executeReturnKey(meldekorteneQuery)

        val query = """
            INSERT INTO UNDERVEIS_PERIODE (perioder_id, periode, utfall, rettighetstype, avslagsarsak,
                                           grenseverdi, timer_arbeid, gradering, meldeperiode, trekk_dagsatser,
                                           andel_arbeidsevne, bruker_av_kvoter, brudd_aktivitetsplikt_id, institusjonsoppholdreduksjon,
                                           meldeplikt_status, meldekort_mottatt)
            VALUES (?, ?::daterange, ?, ?, ?, ?, ?, ?, ?::daterange, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        connection.executeBatch(query, underveisperioder) {
            setParams { periode ->
                setLong(1, perioderId)
                setPeriode(2, periode.periode)
                setEnumName(3, periode.utfall)
                setEnumName(4, periode.rettighetsType)
                setEnumName(5, periode.avslagsårsak)
                setInt(6, periode.grenseverdi.prosentverdi())
                setBigDecimal(7, periode.arbeidsgradering.totaltAntallTimer.antallTimer)
                setInt(8, periode.arbeidsgradering.gradering.prosentverdi())
                setPeriode(9, periode.meldePeriode)
                setInt(10, periode.trekk.antall)
                setInt(11, periode.arbeidsgradering.fastsattArbeidsevne.prosentverdi())
                setArray(12, periode.brukerAvKvoter.map { it.name })
                setLong(13, periode.bruddAktivitetspliktId?.id)
                setInt(14, periode.institusjonsoppholdReduksjon.prosentverdi())
                setEnumName(15, periode.meldepliktStatus)
                setLocalDate(16, periode.arbeidsgradering.opplysningerMottatt)
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

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO UNDERVEIS_GRUNNLAG (behandling_id, perioder_id, sporing_id) SELECT ?, perioder_id, sporing_id from UNDERVEIS_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}