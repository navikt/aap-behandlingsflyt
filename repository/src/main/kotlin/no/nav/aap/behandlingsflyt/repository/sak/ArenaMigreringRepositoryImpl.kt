package no.nav.aap.behandlingsflyt.repository.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.ArenaMigrering
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.ArenaMigreringRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
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
            INSERT INTO ARENA_MIGRERING (sak_id, saksnummer_arena, ident, migrert_tid)
            VALUES (?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, migrering.sakId.toLong())
                setString(2, migrering.saksnummerArena)
                setString(3, migrering.ident)
                setLocalDateTime(4, migrering.migrertTidspunkt)
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
                )
            }
        }
    }
}
