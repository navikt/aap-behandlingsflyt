package no.nav.aap.behandlingsflyt.flyt.internal

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.Tilstand
import no.nav.aap.behandlingsflyt.sak.SakId
import no.nav.aap.behandlingsflyt.sak.Status
import java.time.LocalDateTime

//TODO: Erstatte denne med å flytte kode ut i de relevante Repositoriene.
class FlytOperasjonRepository(private val connection: DBConnection) {

    fun oppdaterSakStatus(sakId: SakId, status: Status) {
        val query = """UPDATE sak SET status = ? WHERE ID = ?"""

        return connection.execute(query) {
            setParams {
                setEnumName(1, status)
                setLong(2, sakId.toLong())
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }

    fun oppdaterBehandlingStatus(behandlingId: BehandlingId, status: no.nav.aap.behandlingsflyt.behandling.Status) {
        val query = """UPDATE behandling SET status = ? WHERE ID = ?"""

        return connection.execute(query) {
            setParams {
                setEnumName(1, status)
                setLong(2, behandlingId.toLong())
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }

    fun leggTilAvklaringsbehov(behandlingId: BehandlingId, definisjoner: List<Definisjon>, funnetISteg: StegType) {
        definisjoner.forEach { definisjon -> leggTilAvklaringsbehov(behandlingId, definisjon, funnetISteg) }
    }

    fun leggTilAvklaringsbehov(behandlingId: BehandlingId, definisjon: Definisjon, funnetISteg: StegType) {
        val avklaringsbehovId = hentRelevantAvklaringsbehov(behandlingId, definisjon, funnetISteg)

        val queryEndring = """
            INSERT INTO AVKLARINGSBEHOV_ENDRING (avklaringsbehov_id, status, begrunnelse, opprettet_av, opprettet_tid) 
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent()

        connection.execute(queryEndring) {
            setParams {
                setLong(1, avklaringsbehovId)
                setEnumName(2, no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Status.OPPRETTET)
                setString(3, "")
                setString(4, "Kelvin")
                setLocalDateTime(5, LocalDateTime.now())
            }
        }
    }

    private fun hentRelevantAvklaringsbehov(
        behandlingId: BehandlingId,
        definisjon: Definisjon,
        funnetISteg: StegType
    ): Long {

        val selectQuery = """
            SELECT id FROM AVKLARINGSBEHOV where behandling_id = ? AND definisjon = ?
        """.trimIndent()

        val avklaringsbehovId = connection.queryFirstOrNull(selectQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
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
                setLong(1, behandlingId.toLong())
                setString(2, definisjon.kode)
                setEnumName(3, funnetISteg)
            }
        }
    }

    fun loggBesøktSteg(behandlingId: BehandlingId, tilstand: Tilstand) {
        val updateQuery = """
            UPDATE STEG_HISTORIKK set aktiv = false WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        connection.execute(updateQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        val query = """
                INSERT INTO STEG_HISTORIKK (behandling_id, steg, status, aktiv, opprettet_tid) 
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, tilstand.steg())
                setEnumName(3, tilstand.status())
                setBoolean(4, true)
                setLocalDateTime(5, LocalDateTime.now())
            }
        }
    }
}
