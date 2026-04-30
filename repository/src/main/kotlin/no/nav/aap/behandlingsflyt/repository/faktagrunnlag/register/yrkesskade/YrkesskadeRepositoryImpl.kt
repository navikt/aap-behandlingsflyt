package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.SkadekombinasjonRegister
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate

class YrkesskadeRepositoryImpl(private val connection: DBConnection) : YrkesskadeRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    companion object : Factory<YrkesskadeRepositoryImpl> {
        override fun konstruer(connection: DBConnection): YrkesskadeRepositoryImpl {
            return YrkesskadeRepositoryImpl(connection)
        }
    }

    private fun eksistererGrunnlag(behandlingId: BehandlingId): Boolean {
        return connection.queryFirstOrNull(
            "SELECT id FROM YRKESSKADE_GRUNNLAG g WHERE g.AKTIV AND g.BEHANDLING_ID = ?"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getLong("id") }
        } != null
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag? {
        val oppgittYrkesskadeISøknad = connection.queryFirstOrNull(
            "SELECT OPPGITT_YRKESSKADE_I_SOKNAD FROM YRKESSKADE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getBoolean("OPPGITT_YRKESSKADE_I_SOKNAD") }
        } ?: false

        return connection.queryList(
            """
            SELECT y.ID AS YRKESSKADE_ID, p.REFERANSE, p.YRKESSKADE_SAKSNUMMER, p.KILDESYSTEM, p.SKADEDATO,
                   p.VEDTAKSDATO, p.SKADEART, p.DIAGNOSE, p.SKADEKOMBINASJONER, p.SKADEKOMBINASJONER_TEKST
            FROM YRKESSKADE_GRUNNLAG g
            INNER JOIN YRKESSKADE y ON g.YRKESSKADE_ID = y.ID
            INNER JOIN YRKESSKADE_DATO p ON y.ID = p.YRKESSKADE_ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                val skadekombinasjonerJson = row.getStringOrNull("SKADEKOMBINASJONER")
                YrkesskadeInternal(
                    id = row.getLong("YRKESSKADE_ID"),
                    ref = row.getString("REFERANSE"),
                    saksnummer = row.getIntOrNull("YRKESSKADE_SAKSNUMMER"),
                    kildesystem = row.getStringOrNull("KILDESYSTEM") ?: "UKJENT",
                    skadedato = row.getLocalDateOrNull("SKADEDATO"),
                    vedtaksdato = row.getLocalDateOrNull("VEDTAKSDATO"),
                    skadeart = row.getStringOrNull("SKADEART"),
                    diagnose = row.getStringOrNull("DIAGNOSE"),
                    skadekombinasjoner = skadekombinasjonerJson
                        ?.let { objectMapper.readValue<List<SkadekombinasjonRegister>>(it) },
                    skadekombinasjonerTekst = row.getStringOrNull("SKADEKOMBINASJONER_TEKST"),
                )
            }
        }.grupperOgMapTilGrunnlag(behandlingId)
            .firstOrNull()
            ?.copy(oppgittYrkesskadeISøknad = oppgittYrkesskadeISøknad)
    }

    private data class YrkesskadeInternal(
        val id: Long,
        val ref: String,
        val saksnummer: Int?,
        val kildesystem: String,
        val skadedato: LocalDate?,
        val vedtaksdato: LocalDate?,
        val skadeart: String?,
        val diagnose: String?,
        val skadekombinasjoner: List<SkadekombinasjonRegister>?,
        val skadekombinasjonerTekst: String?,
    )

    private fun Iterable<YrkesskadeInternal>.grupperOgMapTilGrunnlag(behandlingId: BehandlingId): List<YrkesskadeGrunnlag> {
        return this
            .groupBy(YrkesskadeInternal::id) { yrkesskade ->
                Yrkesskade(
                    ref = yrkesskade.ref,
                    saksnummer = yrkesskade.saksnummer,
                    kildesystem = yrkesskade.kildesystem,
                    skadedato = yrkesskade.skadedato,
                    vedtaksdato = yrkesskade.vedtaksdato,
                    skadeart = yrkesskade.skadeart,
                    diagnose = yrkesskade.diagnose,
                    skadekombinasjoner = yrkesskade.skadekombinasjoner,
                    skadekombinasjonerTekst = yrkesskade.skadekombinasjonerTekst,
                )
            }
            .map { (yrkesskadeId, yrkesskader) ->
                YrkesskadeGrunnlag(
                    id = yrkesskadeId,
                    behandlingId = behandlingId,
                    yrkesskader = Yrkesskader(yrkesskader)
                )
            }
    }

    override fun lagre(behandlingId: BehandlingId, yrkesskader: Yrkesskader?, oppgittYrkesskadeISøknad: Boolean) {
        val yrkesskadeGrunnlag = hentHvisEksisterer(behandlingId)

        if (yrkesskadeGrunnlag?.yrkesskader == yrkesskader) return

        if (eksistererGrunnlag(behandlingId)) {
            deaktiverEksisterende(behandlingId)
        }

        val yrkesskadeId = connection.executeReturnKey("INSERT INTO YRKESSKADE DEFAULT VALUES")

        connection.execute(
            "INSERT INTO YRKESSKADE_GRUNNLAG (BEHANDLING_ID, YRKESSKADE_ID, OPPGITT_YRKESSKADE_I_SOKNAD) VALUES (?, ?, ?)"
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, yrkesskadeId)
                setBoolean(3, oppgittYrkesskadeISøknad)
            }
        }

        if (yrkesskader == null) return

        connection.executeBatch(
            """
            INSERT INTO YRKESSKADE_DATO
                (YRKESSKADE_ID, REFERANSE, YRKESSKADE_SAKSNUMMER, KILDESYSTEM, SKADEDATO,
                 VEDTAKSDATO, SKADEART, DIAGNOSE, SKADEKOMBINASJONER, SKADEKOMBINASJONER_TEKST)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            yrkesskader.yrkesskader
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
                setString(
                    9, yrkesskade.skadekombinasjoner
                        ?.let { objectMapper.writeValueAsString(it) })
                setString(10, yrkesskade.skadekombinasjonerTekst)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val yrkesskadeIds = getYrkesskadeIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from yrkesskade_dato where yrkesskade_id = ANY(?::bigint[]);
            delete from yrkesskade_grunnlag where behandling_id = ?; 
            delete from yrkesskade where id = ANY(?::bigint[]);
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

    private fun getYrkesskadeIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT yrkesskade_id
                    FROM yrkesskade_grunnlag
                    WHERE behandling_id = ?
                      AND yrkesskade_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row -> row.getLong("yrkesskade_id") }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE YRKESSKADE_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams { setLong(1, behandlingId.toLong()) }
            setResultValidator { rowsUpdated -> require(rowsUpdated == 1) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute(
            "INSERT INTO YRKESSKADE_GRUNNLAG (BEHANDLING_ID, YRKESSKADE_ID) SELECT ?, YRKESSKADE_ID FROM YRKESSKADE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?"
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
