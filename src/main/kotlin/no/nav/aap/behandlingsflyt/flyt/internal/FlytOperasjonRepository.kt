package no.nav.aap.behandlingsflyt.flyt.internal

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.Tilstand
import no.nav.aap.behandlingsflyt.sak.SakId
import no.nav.aap.behandlingsflyt.sak.Status
import java.time.LocalDateTime

class FlytOperasjonRepository(private val connection: DBConnection) {

    fun oppdaterSakStatus(sakId: SakId, status: Status) {
        val query = """UPDATE sak SET status = ? WHERE ID = ?"""

        return connection.execute(query) {
            setParams {
                setString(1, status.name)
                setLong(2, sakId.toLong())
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }

    fun oppdaterBehandlingStatus(behandlingId: Long, status: no.nav.aap.behandlingsflyt.behandling.Status) {
        val query = """UPDATE behandling SET status = ? WHERE ID = ?"""

        return connection.execute(query) {
            setParams {
                setString(1, status.name)
                setLong(2, behandlingId)
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }

    fun leggTilAvklaringsbehov(behandlingId: Long, definisjoner: List<Definisjon>, funnetISteg: StegType) {
        definisjoner.forEach { definisjon -> leggTilAvklaringsbehov(behandlingId, definisjon, funnetISteg) }
    }

    fun leggTilAvklaringsbehov(behandlingId: Long, definisjon: Definisjon, funnetISteg: StegType) {
        val avklaringsbehovId = hentRelevantAvklaringsbehov(behandlingId, definisjon, funnetISteg)

        val queryEndring = """
            INSERT INTO AVKLARINGSBEHOV_ENDRING (avklaringsbehov_id, status, begrunnelse, opprettet_av, opprettet_tid) 
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent()

        connection.execute(queryEndring) {
            setParams {
                setLong(1, avklaringsbehovId)
                setString(2, no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Status.OPPRETTET.name)
                setString(3, "")
                setString(4, "Kelvin")
                setLocalDateTime(5, LocalDateTime.now())
            }
        }
    }

    private fun hentRelevantAvklaringsbehov(
        behandlingId: Long,
        definisjon: Definisjon,
        funnetISteg: StegType
    ): Long {

        val selectQuery = """
            SELECT id FROM AVKLARINGSBEHOV where behandling_id = ? AND definisjon = ?
        """.trimIndent()

        val avklaringsbehovId = connection.queryFirstOrNull(selectQuery) {
            setParams {
                setLong(1, behandlingId)
                setString(2, definisjon.kode)
            }
            setRowMapper {
                it.getLong("id")
            }
        }

        if (avklaringsbehovId != null) {
            return avklaringsbehovId
        }

        val query = """
                INSERT INTO AVKLARINGSBEHOV (behandling_id, definisjon, funnet_i_steg) 
                VALUES (?, ?, ?)
                """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setLong(1, behandlingId)
                setString(2, definisjon.kode)
                setString(3, funnetISteg.name)
            }
        }
    }

    fun loggBesøktSteg(behandlingId: Long, tilstand: Tilstand) {
        val updateQuery = """
            UPDATE STEG_HISTORIKK set aktiv = false WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        connection.execute(updateQuery) {
            setParams {
                setLong(1, behandlingId)
            }
        }

        val query = """
                INSERT INTO STEG_HISTORIKK (behandling_id, steg, status, aktiv, opprettet_tid) 
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId)
                setString(2, tilstand.steg().name)
                setString(3, tilstand.status().name)
                setBoolean(4, true)
                setLocalDateTime(5, LocalDateTime.now())
            }
        }
    }
}
