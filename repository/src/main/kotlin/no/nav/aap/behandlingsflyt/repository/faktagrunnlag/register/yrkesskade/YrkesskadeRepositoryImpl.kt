package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.BackfillKandidat
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.SkadekombinasjonRegister
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class YrkesskadeRepositoryImpl(private val connection: DBConnection) : YrkesskadeRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<YrkesskadeRepositoryImpl> {
        override fun konstruer(connection: DBConnection) = YrkesskadeRepositoryImpl(connection)
    }

    private fun eksistererGrunnlag(behandlingId: BehandlingId): Boolean =
        connection.queryFirstOrNull("SELECT id FROM YRKESSKADE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getLong("id") }
        } != null

    override fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag? {
        data class GrunnlagMeta(
            val grunnlagId: Long,
            val yrkesskadeId: Long?,
            val oppgittYrkesskadeISøknad: Boolean?,
        )

        val meta = connection.queryFirstOrNull<GrunnlagMeta>(
            """
                    SELECT g.ID AS GRUNNLAG_ID, g.YRKESSKADE_ID, g.OPPGITT_YRKESSKADE_I_SOKNAD
                    FROM YRKESSKADE_GRUNNLAG g
                    WHERE g.AKTIV AND g.BEHANDLING_ID = ?
                    """
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper {
                GrunnlagMeta(
                    grunnlagId = it.getLong("GRUNNLAG_ID"),
                    yrkesskadeId = it.getLongOrNull("YRKESSKADE_ID"),
                    oppgittYrkesskadeISøknad = it.getBooleanOrNull("OPPGITT_YRKESSKADE_I_SOKNAD"),
                )
            }
        } ?: return null

        val yrkesskader = meta.yrkesskadeId?.let { yrkesskadeId ->
            connection.queryList(
                """
                SELECT p.REFERANSE, p.YRKESSKADE_SAKSNUMMER, p.KILDESYSTEM, p.SKADEDATO, p.VEDTAKSDATO, p.SKADEART, p.DIAGNOSE, p.SKADEKOMBINASJONER, p.SKADEKOMBINASJONER_TEKST
                FROM YRKESSKADE_DATO p
                WHERE p.YRKESSKADE_ID = ?
                """
            ) {
                setParams { setLong(1, yrkesskadeId) }
                setRowMapper { row ->
                    Yrkesskade(
                        ref = row.getString("REFERANSE"),
                        saksnummer = row.getIntOrNull("YRKESSKADE_SAKSNUMMER"),
                        kildesystem = row.getStringOrNull("KILDESYSTEM") ?: "UKJENT",
                        skadedato = row.getLocalDateOrNull("SKADEDATO"),
                        vedtaksdato = row.getLocalDateOrNull("VEDTAKSDATO"),
                        skadeart = row.getStringOrNull("SKADEART"),
                        diagnose = row.getStringOrNull("DIAGNOSE"),
                        skadekombinasjoner = row.getStringOrNull("SKADEKOMBINASJONER")
                            ?.let { SkadekombinasjonRegister.fromString(it)?.let { listOf(it) } } ?: emptyList(),
                        skadekombinasjonerTekst = row.getStringOrNull("SKADEKOMBINASJONER_TEKST"),
                    )
                }
            }
        } ?: emptyList()

        return YrkesskadeGrunnlag(
            id = meta.grunnlagId,
            behandlingId = behandlingId,
            yrkesskader = Yrkesskader(yrkesskader),
            oppgittYrkesskadeISøknad = meta.oppgittYrkesskadeISøknad,
        )
    }

    override fun lagre(
        behandlingId: BehandlingId,
        registerYrkesskader: Yrkesskader?,
        oppgittYrkesskadeISøknad: Boolean?
    ) {
        val gamleData = hentHvisEksisterer(behandlingId)
        if (gamleData?.yrkesskader == registerYrkesskader &&
            gamleData?.oppgittYrkesskadeISøknad == oppgittYrkesskadeISøknad
        ) return

        if (eksistererGrunnlag(behandlingId)) {
            deaktiverEksisterende(behandlingId)
        }

        val yrkesskadeId: Long? = registerYrkesskader
            ?.yrkesskader
            ?.takeIf { it.isNotEmpty() }
            ?.let { connection.executeReturnKey("INSERT INTO YRKESSKADE DEFAULT VALUES") }

        // Grunnlag kobler de to — begge kan være null uavhengig av hverandre
        connection.execute(
            "INSERT INTO YRKESSKADE_GRUNNLAG (BEHANDLING_ID, YRKESSKADE_ID, OPPGITT_YRKESSKADE_I_SOKNAD) VALUES (?, ?, ?)"
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, yrkesskadeId)   // nullable — setter NULL hvis ingen registerdata
                setBoolean(3, oppgittYrkesskadeISøknad)
            }
        }

        // Lagre enkeltradene i YRKESSKADE_DATO kun hvis vi fikk en YRKESSKADE-rad
        if (yrkesskadeId != null) {
            connection.executeBatch(
                "INSERT INTO YRKESSKADE_DATO (YRKESSKADE_ID, REFERANSE, YRKESSKADE_SAKSNUMMER, KILDESYSTEM, SKADEDATO, VEDTAKSDATO, SKADEART, DIAGNOSE, SKADEKOMBINASJONER, SKADEKOMBINASJONER_TEKST, OPPRETTET_TID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                registerYrkesskader.yrkesskader
            ) {
                setParams { yrkesskade ->
                    setLong(1, yrkesskadeId)
                    setString(2, yrkesskade.ref)
                    setInt(3, yrkesskade.saksnummer)
                    setString(4, yrkesskade.kildesystem)
                    setLocalDate(5, yrkesskade.skadedato)
                    setLocalDate(6, yrkesskade.vedtaksdato)
                    setString(7, yrkesskade.skadeart)
                    setString(8, yrkesskade.diagnose)
                    setString(9, yrkesskade.skadekombinasjoner?.joinToString(","))
                    setString(10, yrkesskade.skadekombinasjonerTekst)
                    setLocalDateTime(11, LocalDateTime.now())
                }
            }
        }
    }

    override fun hentKandidaterForBackfill(): List<BackfillKandidat> =
        connection.queryList(
            """
        SELECT yd.id AS yrkesskade_dato_id, yg.behandling_id,
               yd.referanse, yd.yrkesskade_saksnummer, yd.kildesystem, yd.skadedato
        FROM yrkesskade_dato yd
        JOIN yrkesskade y           ON y.id = yd.yrkesskade_id
        JOIN yrkesskade_grunnlag yg ON yg.yrkesskade_id = y.id AND yg.aktiv = true
        WHERE yd.skadeart IS NULL
        """
        ) {
            setRowMapper { row ->
                BackfillKandidat(
                    yrkesskadeDatoId = row.getLong("yrkesskade_dato_id"),
                    behandlingId = BehandlingId(row.getLong("behandling_id")),
                    ref = row.getString("referanse"),
                    saksnummer = row.getIntOrNull("yrkesskade_saksnummer"),
                    kildesystem = row.getStringOrNull("kildesystem") ?: "UKJENT",
                    skadedato = row.getLocalDateOrNull("skadedato"),
                )
            }
        }

    override fun backfillYrkesskadeDato(yrkesskadeDatoId: Long, yrkesskade: Yrkesskade) {
        connection.execute(
            """
        UPDATE yrkesskade_dato
        SET vedtaksdato              = ?,
            skadeart                 = ?,
            diagnose                 = ?,
            skadekombinasjoner       = ?,
            skadekombinasjoner_tekst = ?
        WHERE id = ?
        """
        ) {
            setParams {
                setLocalDate(1, yrkesskade.vedtaksdato)
                setString(2, yrkesskade.skadeart)
                setString(3, yrkesskade.diagnose)
                setString(4, yrkesskade.skadekombinasjoner?.joinToString(","))
                setString(5, yrkesskade.skadekombinasjonerTekst)
                setLong(6, yrkesskadeDatoId)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val yrkesskadeIds = getYrkesskadeIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            DELETE FROM YRKESSKADE_DATO WHERE YRKESSKADE_ID = ANY(?::bigint[]);
            DELETE FROM YRKESSKADE_GRUNNLAG WHERE BEHANDLING_ID = ?;
            DELETE FROM YRKESSKADE WHERE ID = ANY(?::bigint[]);
            """.trimIndent()
        ) {
            setParams {
                setLongArray(1, yrkesskadeIds)
                setLong(2, behandlingId.id)
                setLongArray(3, yrkesskadeIds)
            }
        }

        log.info("Slettet $deletedRows rader fra yrkesskade_grunnlag")
    }

    private fun getYrkesskadeIds(behandlingId: BehandlingId): List<Long> =
        connection.queryList(
            "SELECT YRKESSKADE_ID FROM YRKESSKADE_GRUNNLAG WHERE BEHANDLING_ID = ? AND YRKESSKADE_ID IS NOT NULL"
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { it.getLong("YRKESSKADE_ID") }
        }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE YRKESSKADE_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams { setLong(1, behandlingId.toLong()) }
            setResultValidator { require(it == 1) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute(
            "INSERT INTO YRKESSKADE_GRUNNLAG (BEHANDLING_ID, YRKESSKADE_ID, OPPGITT_YRKESSKADE_I_SOKNAD) SELECT ?, YRKESSKADE_ID, OPPGITT_YRKESSKADE_I_SOKNAD FROM YRKESSKADE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?"
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
