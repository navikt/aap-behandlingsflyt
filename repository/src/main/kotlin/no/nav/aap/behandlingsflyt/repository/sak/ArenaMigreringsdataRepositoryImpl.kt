package no.nav.aap.behandlingsflyt.repository.sak

import no.nav.aap.behandlingsflyt.arena.ArenaMigreringsdata
import no.nav.aap.behandlingsflyt.arena.ArenaMigreringsdataRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.Factory
import java.time.Instant

class ArenaMigreringsdataRepositoryImpl(private val connection: DBConnection) : ArenaMigreringsdataRepository {

    companion object : Factory<ArenaMigreringsdataRepositoryImpl> {
        override fun konstruer(connection: DBConnection): ArenaMigreringsdataRepositoryImpl {
            return ArenaMigreringsdataRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, steg: StegType, data: Any, hentetTidspunkt: Instant) {
        val json = DefaultJsonMapper.toJson(data)
        if (erLikAktivRad(behandlingId, steg, json)) return

        connection.execute(
            """
            UPDATE ARENA_MIGRERINGSDATA SET AKTIV = FALSE
            WHERE BEHANDLING_ID = ? AND STEG = ? AND AKTIV = TRUE
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, steg.name)
            }
        }
        connection.execute(
            """
            INSERT INTO ARENA_MIGRERINGSDATA (BEHANDLING_ID, STEG, HENTET_TIDSPUNKT, DATA)
            VALUES (?, ?, ?, ?::jsonb)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, steg.name)
                setInstant(3, hentetTidspunkt)
                setString(4, json)
            }
        }
    }

    // jsonb-likhet ignorerer nøkkelrekkefølge og whitespace, men ikke rekkefølge i lister.
    private fun erLikAktivRad(behandlingId: BehandlingId, steg: StegType, json: String): Boolean {
        return connection.queryFirstOrNull(
            """
            SELECT 1 AS finnes FROM ARENA_MIGRERINGSDATA
            WHERE BEHANDLING_ID = ? AND STEG = ? AND AKTIV = TRUE AND DATA = ?::jsonb
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, steg.name)
                setString(3, json)
            }
            setRowMapper { true }
        } ?: false
    }

    override fun hentAktivHvisEksisterer(behandlingId: BehandlingId, steg: StegType): ArenaMigreringsdata? {
        return connection.queryFirstOrNull(
            """
            SELECT * FROM ARENA_MIGRERINGSDATA
            WHERE BEHANDLING_ID = ? AND STEG = ? AND AKTIV = TRUE
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, steg.name)
            }
            setRowMapper { row ->
                ArenaMigreringsdata(
                    behandlingId = BehandlingId(row.getLong("behandling_id")),
                    steg = StegType.valueOf(row.getString("steg")),
                    hentetTidspunkt = row.getInstant("hentet_tidspunkt"),
                    data = row.getString("data"),
                )
            }
        }
    }
}
