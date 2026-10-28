package no.nav.aap.behandlingsflyt

import net.logstash.logback.marker.Markers.appendEntries
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetService
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.VirksomhetEtableringResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.brukteDager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.gjeldendeFasevurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.tilGammeltFormat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("no.nav.aap.behandlingsflyt.BackfillEtableringVurderinger")
private val teamLogs = LoggerFactory.getLogger("team-logs")

private fun List<EtableringEgenVirksomhetVurdering>.vurderingsutfall() =
    gjeldendeFasevurderinger()
        .filter { it.fase != null }
        .gjeldendeVurderinger()
        .segmenter().map { it.periode to it.verdi.copy(fom = it.fom(), tom = it.tom()) }

private fun EtableringEgenVirksomhetVurdering.tilDiagnostikk(): Map<String, Any?> = mapOf(
    "vurdertIBehandling" to vurdertIBehandling.toLong(),
    "opprettet" to opprettet.toString(),
    "fom" to fom.toString(),
    "tom" to tom?.toString(),
    "fase" to fase?.name,
    "versjon" to versjon.name,
    "utviklingsPerioder" to utviklingsPerioder.map {
        mapOf("fom" to it.fom.toString(), "tom" to it.tom.toString())
    },
    "oppstartsPerioder" to oppstartsPerioder.map {
        mapOf("fom" to it.fom.toString(), "tom" to it.tom.toString())
    },
)

/** Deler historiske etableringsvurderinger opp i én vurdering per periode. */
class BackfillEtableringVurderinger(
    private val dataSource: DataSource,
    private val dryRun: Boolean,
) {
    data class Resultat(
        val antallBehandlinger: Int = 0,
        val antallVurderingerSomSplittes: Int = 0,
        val antallNyeVurderinger: Int = 0,
        val antallMedAvvikIVarighet: Int = 0,
    ) {
        operator fun plus(other: Resultat) = Resultat(
            antallBehandlinger + other.antallBehandlinger,
            antallVurderingerSomSplittes + other.antallVurderingerSomSplittes,
            antallNyeVurderinger + other.antallNyeVurderinger,
            antallMedAvvikIVarighet + other.antallMedAvvikIVarighet,
        )
    }


    @Suppress("TooGenericExceptionCaught")
    fun kjør() {
        Thread.ofVirtual()
            .name("backfillEtableringVurderinger")
            .start {
                while (true) {
                    if (isLeader(log)) {
                        log.info("Begynner backfill av etableringsvurderinger, dryRun=$dryRun")
                        try {
                            val resultat = dataSource.transaction { connection ->
                                migrer(connection)
                            }
                            log.info("Backfill av etableringsvurderinger fullført, dryRun=$dryRun, $resultat")
                        } catch (e: Exception) {
                            log.warn(
                                "BackfillEtableringVurderinger: uncaughtException {}, se secure / team log",
                                e.javaClass.name
                            )
                            teamLogs.warn(
                                "BackfillEtableringVurderinger: uncaughtException {}: {}",
                                e.javaClass.name,
                                e.message,
                                e
                            )
                        }
                    }
                    Thread.sleep(Duration.ofMinutes(5))
                }
            }
    }

    fun migrer(connection: DBConnection): Resultat {
        val alleSomTrengerMigrering: List<BehandlingId> = run {

            val sql = """
            SELECT DISTINCT behandling_id FROM etablering_egen_virksomhet_grunnlag
            """.trimIndent()
            connection.queryList(sql) {
                setRowMapper { row ->
                    BehandlingId(row.getLong("behandling_id"))
                }
            }
        }
        log.info("Fant ${alleSomTrengerMigrering.size} behandlinger som trenger migrering")

        return alleSomTrengerMigrering
            .map { behandlingId ->
                val analyse = analyserBehandling(connection, behandlingId)
                if (analyse.antallMedAvvikIVarighet > 0) {
                    log.warn("Avvik i varighet etter migrering for behandling ${behandlingId.id}")
                }
                if (!dryRun) {
                    migrerBehandling(connection, behandlingId)
                }
                analyse
            }
            .fold(Resultat(), Resultat::plus)
    }

    fun analyserBehandling(connection: DBConnection, behandlingId: BehandlingId): Resultat {
        return hentAlleGrunnlag(connection, behandlingId)
            .map { grunnlagId ->
                val vurderinger = vurderinger(connection, grunnlagId)
                val migreringsplan = lagMigreringsplan(vurderinger)
                val splittede = migreringsplan.filter { it.skalErstattes }
                val før = vurderinger.map { it.second }
                val etter = migreringsplan.flatMap { it.vurderingerEtterMigrering }
                val varighetFør = varighet(før)
                val varighetEtter = varighet(etter)
                val harAvvik = varighetFør != varighetEtter
                if (harAvvik) {
                    val diagnose = mapOf(
                        "dryRun" to dryRun,
                        "behandlingId" to behandlingId.toLong(),
                        "grunnlagId" to grunnlagId,
                        "vurderingerId" to connection.hentVurderingerGruppeId(vurderinger.first().first),
                        "varighetFør" to mapOf("utvikling" to varighetFør.first, "oppstart" to varighetFør.second),
                        "varighetEtter" to mapOf("utvikling" to varighetEtter.first, "oppstart" to varighetEtter.second),
                        "migreringsplan" to migreringsplan.map { steg ->
                            mapOf(
                                "vurderingId" to steg.vurderingId,
                                "skalErstattes" to steg.skalErstattes,
                                "opprinneligVurdering" to steg.opprinneligVurdering.tilDiagnostikk(),
                                "oppdaterteVurderinger" to steg.oppdaterteVurderinger.map { it.tilDiagnostikk() },
                            )
                        },
                    )
                    teamLogs.warn(
                        appendEntries(diagnose),
                        "Avvik i varighet ved backfill av etableringsvurderinger",
                    )
                }
                Resultat(
                    antallVurderingerSomSplittes = splittede.size,
                    antallNyeVurderinger = splittede.sumOf { it.oppdaterteVurderinger.size },
                    antallMedAvvikIVarighet = if (harAvvik) 1 else 0,
                )
            }
            .fold(Resultat(antallBehandlinger = 1), Resultat::plus)
    }

    private fun varighet(vurderinger: List<EtableringEgenVirksomhetVurdering>): Pair<Int, Int> =
        vurderinger
            .map { if (it.fase != null) it.tilGammeltFormat() else it }
            .brukteDager()

    fun hentAlleGrunnlag(connection: DBConnection, behandlingId: BehandlingId): List<Long> {
        val sql = """
                SELECT id FROM etablering_egen_virksomhet_grunnlag g
                WHERE g.behandling_id = ?
            """.trimIndent()

        return connection.queryList(sql) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                it.getLong("id")
            }
        }
    }

    data class PeriodeTemp(val fom: LocalDate, val to: LocalDate)

    fun vurderinger(connection: DBConnection, grunnlagId: Long): List<Pair<Long, EtableringEgenVirksomhetVurdering>> {
        val vurderingerId: Long = connection.queryFirstOrNull(
            """
                SELECT vurderinger_id
                FROM etablering_egen_virksomhet_grunnlag
                WHERE id = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, grunnlagId)
            }
            setRowMapper { it.getLong("vurderinger_id") }
        } ?: return emptyList()

        val vurderingSql = """
            select v.*,
                   coalesce(utvikling.utv_perioder, '[]'::json) as utv_perioder,
                   coalesce(oppstart.oppst_perioder, '[]'::json) as oppst_perioder
            from ETABLERING_EGEN_VIRKSOMHET_VURDERING v
                     left join lateral ( select json_agg(json_build_object('fom', lower(evup.periode), 'to',
                                                                           upper(evup.periode))) as utv_perioder
                                         from egen_virksomhet_utvikling_perioder utvs
                                                  join egen_virksomhet_utvikling_periode evup
                                                       on utvs.id = evup.perioder_id
                                         where utvs.id = v.egen_virksomhet_utvikling_perioder_id) utvikling on true
                     left join lateral (select json_agg(json_build_object('fom', lower(evop.periode), 'to',
                                                                          upper(evop.periode))) as oppst_perioder
                                        from egen_virksomhet_oppstart_perioder opps
                                                 join egen_virksomhet_oppstart_periode evop
                                                      on opps.id = evop.perioder_id
                                        where opps.id = v.egen_virksomhet_oppstart_perioder_id) oppstart
                               on true
            where v.vurderinger_id = ?
            order by v.id
        """.trimIndent()

        return connection.queryList(vurderingSql) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper { row ->
                row.getLong("id") to EtableringEgenVirksomhetVurdering(
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    virksomhetNavn = row.getString("VIRKSOMHET_NAVN"),
                    orgNr = row.getStringOrNull("ORG_NR"),
                    foreliggerFagligVurdering = row.getBoolean("FORELIGGER_FAGLIG_VURDERING"),
                    virksomhetErNy = row.getBooleanOrNull("VIRKSOMHET_ER_NY"),
                    brukerEierVirksomheten = row.getEnumOrNull("BRUKER_EIER_VIRKSOMHET"),
                    kanFøreTilSelvforsørget = row.getBooleanOrNull("KAN_BLI_SELVFORSORGET"),
                    utviklingsPerioder = row.getString("utv_perioder").let {
                        DefaultJsonMapper.fromJson<List<PeriodeTemp>>(it)
                    }.map { Periode(it.fom, it.to.minusDays(1)) },
                    oppstartsPerioder = row.getString("oppst_perioder").let {
                        DefaultJsonMapper.fromJson<List<PeriodeTemp>>(it)
                    }.map { Periode(it.fom, it.to.minusDays(1)) },
                    vurdertAv = Bruker(row.getString("VURDERT_AV")),
                    opprettet = row.getInstant("OPPRETTET_TID"),
                    vurdertIBehandling = row.getLong("VURDERT_I_BEHANDLING").let(::BehandlingId),
                    fom = row.getLocalDate("GJELDER_FRA"),
                    tom = row.getLocalDateOrNull("GJELDER_TIL"),
                    fase = row.getEnumOrNull("FASE"),
                    erRegistrertINødvendigeOffentligeRegister = row.getBooleanOrNull("ER_REGISTRERT_I_NODVENDIGE_OFFENTLIGE_REGISTER"),
                    jobberBrukerAktivMedVirksomheten = row.getBooleanOrNull("JOBBER_BRUKER_AKTIVT_MED_VIRKSOMHET"),
                    versjon = row.getEnum("VERSJON"),
                )
            }
        }
    }

    fun oppdaterVurdering(vurdering: EtableringEgenVirksomhetVurdering) = vurdering.tilMigrerteVurderinger()

    fun migrerBehandling(connection: DBConnection, behandlingId: BehandlingId) {
        val alleGrunnlag = hentAlleGrunnlag(connection, behandlingId)
        val service = EtableringEgenVirksomhetService(postgresRepositoryRegistry.provider(connection))

        alleGrunnlag.forEach { grunnlagId ->
            val vurderinger = this.vurderinger(connection, grunnlagId)
            val migreringsplan = lagMigreringsplan(vurderinger)
            val før = vurderinger.map { (_, vurdering) ->
                if (vurdering.fase != null) vurdering.tilGammeltFormat() else vurdering
            }
            val varighetFør = varighet(før)
            val valideringsfeilFør =
                service.erVurderingerGyldig(behandlingId, før) as? VirksomhetEtableringResultat.Ugyldig
            val forventetUtfall = før.vurderingsutfall()
            migreringsplan.filter { it.skalErstattes }.forEach { steg ->
                val vurderingerGruppeId = connection.hentVurderingerGruppeId(steg.vurderingId)
                steg.oppdaterteVurderinger.forEach { oppdatertVurdering ->
                    lagreVurdering(connection, vurderingerGruppeId, oppdatertVurdering)
                }
                slettVurdering(connection, steg.vurderingId)
            }
            val vurderingerEtter = this.vurderinger(connection, grunnlagId)
            val etter = vurderingerEtter.map { (_, vurdering) ->
                if (vurdering.fase != null) vurdering.tilGammeltFormat() else vurdering
            }
            check(varighetFør == varighet(etter)) {
                "Varighet endret ved migrering av behandling ${behandlingId.id}, grunnlag $grunnlagId"
            }
            val valideringsfeilEtter =
                service.erVurderingerGyldig(behandlingId, etter) as? VirksomhetEtableringResultat.Ugyldig
            check(valideringsfeilFør == valideringsfeilEtter) {
                "Valideringsresultat endret ved migrering av behandling ${behandlingId.id}, grunnlag $grunnlagId"
            }
            check(forventetUtfall == etter.vurderingsutfall()) {
                "Vurderingsutfall endret ved migrering av behandling ${behandlingId.id}, grunnlag $grunnlagId"
            }
        }
    }

    private fun lagreVurdering(
        connection: DBConnection,
        vurderingerId: Long,
        vurdering: EtableringEgenVirksomhetVurdering,
    ) {
        require(vurdering.utviklingsPerioder.isEmpty() && vurdering.oppstartsPerioder.isEmpty()) {
            "Vurderinger i nytt format skal ikke ha perioder"
        }
        connection.execute(
            """
                INSERT INTO ETABLERING_EGEN_VIRKSOMHET_VURDERING (
                    BEGRUNNELSE, FORELIGGER_FAGLIG_VURDERING, VIRKSOMHET_ER_NY, BRUKER_EIER_VIRKSOMHET,
                    KAN_BLI_SELVFORSORGET, VIRKSOMHET_NAVN, ORG_NR, EGEN_VIRKSOMHET_UTVIKLING_PERIODER_ID,
                    EGEN_VIRKSOMHET_OPPSTART_PERIODER_ID, VURDERINGER_ID, VURDERT_I_BEHANDLING, VURDERT_AV,
                    GJELDER_FRA, GJELDER_TIL, OPPRETTET_TID, FASE, ER_REGISTRERT_I_NODVENDIGE_OFFENTLIGE_REGISTER,
                    JOBBER_BRUKER_AKTIVT_MED_VIRKSOMHET, VERSJON
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.foreliggerFagligVurdering)
                setBoolean(3, vurdering.virksomhetErNy)
                setEnumName(4, vurdering.brukerEierVirksomheten)
                setBoolean(5, vurdering.kanFøreTilSelvforsørget)
                setString(6, vurdering.virksomhetNavn)
                setString(7, vurdering.orgNr)
                setLong(8, null)
                setLong(9, null)
                setLong(10, vurderingerId)
                setLong(11, vurdering.vurdertIBehandling.toLong())
                setString(12, vurdering.vurdertAv.ident)
                setLocalDate(13, vurdering.fom)
                setLocalDate(14, vurdering.tom)
                setInstant(15, vurdering.opprettet)
                setEnumName(16, vurdering.fase)
                setBoolean(17, vurdering.erRegistrertINødvendigeOffentligeRegister)
                setBoolean(18, vurdering.jobberBrukerAktivMedVirksomheten)
                setEnumName(19, vurdering.versjon)
            }
        }
    }

    private fun slettVurdering(
        connection: DBConnection,
        vurderingId: Long,
    ) {
        val perioderIder: Pair<Long?, Long?> = connection.queryFirstOrNull(
            """
                SELECT egen_virksomhet_utvikling_perioder_id, egen_virksomhet_oppstart_perioder_id
                FROM etablering_egen_virksomhet_vurdering
                WHERE id = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, vurderingId) }
            setRowMapper {
                it.getLongOrNull("egen_virksomhet_utvikling_perioder_id") to
                        it.getLongOrNull("egen_virksomhet_oppstart_perioder_id")
            }
        } ?: error("Fant ikke vurdering $vurderingId")

        connection.execute(
            "DELETE FROM ETABLERING_EGEN_VIRKSOMHET_VURDERING WHERE id = ?"
        ) {
            setParams { setLong(1, vurderingId) }
        }
        slettUtviklingsperioder(connection, perioderIder.first)
        slettOppstartsperioder(connection, perioderIder.second)
    }

    private fun slettUtviklingsperioder(connection: DBConnection, perioderId: Long?) {
        if (perioderId == null) return
        connection.execute(
            """
                DELETE FROM egen_virksomhet_utvikling_periode ep
                WHERE ep.perioder_id = ?
                  AND NOT EXISTS (
                    SELECT 1 FROM etablering_egen_virksomhet_vurdering v
                    WHERE v.egen_virksomhet_utvikling_perioder_id = ep.perioder_id
                  )
            """.trimIndent()
        ) {
            setParams { setLong(1, perioderId) }
        }
        connection.execute(
            """
                DELETE FROM egen_virksomhet_utvikling_perioder p
                WHERE p.id = ?
                  AND NOT EXISTS (
                    SELECT 1 FROM etablering_egen_virksomhet_vurdering v
                    WHERE v.egen_virksomhet_utvikling_perioder_id = p.id
                  )
            """.trimIndent()
        ) {
            setParams { setLong(1, perioderId) }
        }
    }

    private fun slettOppstartsperioder(connection: DBConnection, perioderId: Long?) {
        if (perioderId == null) return
        connection.execute(
            """
                DELETE FROM egen_virksomhet_oppstart_periode ep
                WHERE ep.perioder_id = ?
                  AND NOT EXISTS (
                    SELECT 1 FROM etablering_egen_virksomhet_vurdering v
                    WHERE v.egen_virksomhet_oppstart_perioder_id = ep.perioder_id
                  )
            """.trimIndent()
        ) {
            setParams { setLong(1, perioderId) }
        }
        connection.execute(
            """
                DELETE FROM egen_virksomhet_oppstart_perioder p
                WHERE p.id = ?
                  AND NOT EXISTS (
                    SELECT 1 FROM etablering_egen_virksomhet_vurdering v
                    WHERE v.egen_virksomhet_oppstart_perioder_id = p.id
                  )
            """.trimIndent()
        ) {
            setParams { setLong(1, perioderId) }
        }
    }
}
