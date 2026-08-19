package no.nav.aap.behandlingsflyt.repository.sak

import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakMedVedtakResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.ArenaMigrering
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.ArenaMigreringRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.Factory

class ArenaMigreringRepositoryImpl(private val connection: DBConnection) : ArenaMigreringRepository {

    companion object : Factory<ArenaMigreringRepositoryImpl> {
        override fun konstruer(connection: DBConnection): ArenaMigreringRepositoryImpl {
            return ArenaMigreringRepositoryImpl(connection)
        }
    }

    override fun lagre(migrering: ArenaMigrering) {
        connection.execute(
            """
            INSERT INTO ARENA_MIGRERING (sak_id, saksnummer_arena, ident, migrert_tid, arena_sak_data)
            VALUES (?, ?, ?, ?, ?::jsonb)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, migrering.sakId.toLong())
                setString(2, migrering.saksnummerArena)
                setString(3, migrering.ident)
                setLocalDateTime(4, migrering.migrertTidspunkt)
                setString(5, migrering.arenaSakData?.let { DefaultJsonMapper.toJson(it) })
            }
        }
    }

    override fun lagreArenaSakData(sakId: SakId, data: ArenaSakMedVedtakResponse) {
        connection.execute(
            """
            UPDATE ARENA_MIGRERING SET arena_sak_data = ?::jsonb WHERE sak_id = ?
            """.trimIndent()
        ) {
            setParams {
                setString(1, DefaultJsonMapper.toJson(data))
                setLong(2, sakId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1) {
                    "Fant ingen arenamigrering å lagre arenasak-data på for sak $sakId"
                }
            }
        }
    }

    override fun hentForSakHvisEksisterer(sakId: SakId): ArenaMigrering? {
        return connection.queryFirstOrNull(
            """
            SELECT * FROM ARENA_MIGRERING WHERE sak_id = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper { row ->
                ArenaMigrering(
                    sakId = SakId(row.getLong("sak_id")),
                    saksnummerArena = row.getString("saksnummer_arena"),
                    ident = row.getString("ident"),
                    migrertTidspunkt = row.getLocalDateTime("migrert_tid"),
                    arenaSakData = row.getStringOrNull("arena_sak_data")
                        ?.let { DefaultJsonMapper.fromJson<ArenaSakMedVedtakResponse>(it) },
                )
            }
        }
    }
}
