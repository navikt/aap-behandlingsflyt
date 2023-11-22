package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import java.time.LocalDateTime

class AvklaringsbehovRepository(private val connection: DBConnection) {

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

        return opprettAvklaringsbehov(behandlingId, definisjon, funnetISteg)
    }

    private fun opprettAvklaringsbehov(
        behandlingId: BehandlingId,
        definisjon: Definisjon,
        funnetISteg: StegType
    ): Long {
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

    fun løs(behandlingId: BehandlingId, definisjon: Definisjon, begrunnelse: String, kreverToTrinn: Boolean?) {
        val avklaringsbehovene = hent(behandlingId)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)

        if (avklaringsbehov == null) {
            throw IllegalStateException("Fant ikke avklaringsbehov med $definisjon for behandling $behandlingId")
        }

        if (kreverToTrinn != null) {
            val query = """
            UPDATE AVKLARINGSBEHOV SET krever_to_trinn = ? WHERE id = ?
            """.trimIndent()

            connection.execute(query) {
                setParams {
                    setLong(1, avklaringsbehov.id)
                    setBoolean(2, kreverToTrinn)
                }
            }
        }

        val query = """
            INSERT INTO AVKLARINGSBEHOV_ENDRING (avklaringsbehov_id, status, begrunnelse, opprettet_av, opprettet_tid) 
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, avklaringsbehov.id)
                setEnumName(2, Status.AVSLUTTET)
                setString(3, begrunnelse)
                setString(4, "Saksbehandler") // TODO: Hent fra sikkerhetscontex
                setLocalDateTime(5, LocalDateTime.now()) // TODO: Hent fra sikkerhetscontex
            }
        }
    }

    fun toTrinnsVurdering(behandlingId: BehandlingId, definisjon: Definisjon, begrunnelse: String, godkjent: Boolean) {
        val avklaringsbehovene = hent(behandlingId)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)

        if (avklaringsbehov == null) {
            throw IllegalStateException("Fant ikke avklaringsbehov med $definisjon for behandling $behandlingId")
        }
        if (!(avklaringsbehov.erTotrinn() && avklaringsbehov.status() == Status.AVSLUTTET)) {
            throw IllegalStateException("Har ikke rett tilstand på avklaringsbehov")
        }

        val query = """
            INSERT INTO AVKLARINGSBEHOV_ENDRING (avklaringsbehov_id, status, begrunnelse, opprettet_av, opprettet_tid) 
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent()

        val status = if (godkjent) {
            Status.TOTRINNS_VURDERT
        } else {
            Status.SENDT_TILBAKE_FRA_BESLUTTER
        }

        connection.execute(query) {
            setParams {
                setLong(1, avklaringsbehov.id)
                setEnumName(2, status)
                setString(3, begrunnelse)
                setString(4, "Saksbehandler") // TODO: Hent fra sikkerhetscontex
                setLocalDateTime(5, LocalDateTime.now())
            }
        }
    }

    fun hent(behandlingId: BehandlingId): Avklaringsbehovene {
        val query = """
            SELECT * FROM AVKLARINGSBEHOV WHERE behandling_id = ?
            """.trimIndent()

        return Avklaringsbehovene(connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                mapAvklaringsbehov(it)
            }
        })
    }

    private fun mapAvklaringsbehov(row: Row): Avklaringsbehov {
        val definisjon = Definisjon.forKode(row.getString("definisjon"))
        val id = row.getLong("id")
        return Avklaringsbehov(
            id = id,
            definisjon = definisjon,
            funnetISteg = StegType.valueOf(row.getString("funnet_i_steg")),
            kreverToTrinn = row.getBooleanOrNull("krever_to_trinn"),
            historikk = hentEndringer(id).toMutableList()
        )
    }

    private fun hentEndringer(avklaringsbehovId: Long): List<Endring> {
        val query = """
            SELECT * FROM AVKLARINGSBEHOV_ENDRING 
            WHERE avklaringsbehov_id = ? 
            ORDER BY opprettet_tid ASC
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, avklaringsbehovId)
            }
            setRowMapper {
                mapEndringer(it)
            }
        }
    }

    private fun mapEndringer(row: Row): Endring {
        return Endring(
            status = Status.valueOf(row.getString("status")),
            tidsstempel = row.getLocalDateTime("opprettet_tid"),
            begrunnelse = row.getString("begrunnelse"),
            endretAv = row.getString("opprettet_av")
        )
    }
}
