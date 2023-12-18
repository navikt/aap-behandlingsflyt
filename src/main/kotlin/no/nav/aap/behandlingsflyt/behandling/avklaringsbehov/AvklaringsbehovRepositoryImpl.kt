package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import java.time.LocalDateTime

class AvklaringsbehovRepositoryImpl(private val connection: DBConnection) : AvklaringsbehovRepository, AvklaringsbehovOperasjonerRepository {

    override fun leggTilAvklaringsbehov(
        behandlingId: BehandlingId,
        definisjoner: List<Definisjon>,
        funnetISteg: StegType
    ) {
        definisjoner.forEach { definisjon -> leggTilAvklaringsbehov(behandlingId, definisjon, funnetISteg) }
    }

    override fun leggTilAvklaringsbehov(behandlingId: BehandlingId, definisjon: Definisjon, funnetISteg: StegType) {
        var avklaringsbehovId = hentRelevantAvklaringsbehov(behandlingId, definisjon)

        if (avklaringsbehovId == null) {
            avklaringsbehovId = opprettAvklaringsbehov(behandlingId, definisjon, funnetISteg)
        }

        opprettAvklaringsbehovEndring(avklaringsbehovId, Status.OPPRETTET, "", "Kelvin")
    }

    private fun hentRelevantAvklaringsbehov(
        behandlingId: BehandlingId,
        definisjon: Definisjon
    ): Long? {

        val selectQuery = """
            SELECT id FROM AVKLARINGSBEHOV where behandling_id = ? AND definisjon = ?
        """.trimIndent()

        return connection.queryFirstOrNull<Long>(selectQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, definisjon.kode)
            }
            setRowMapper {
                it.getLong("id")
            }
        }
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

    override fun løs(behandlingId: BehandlingId, definisjon: Definisjon, begrunnelse: String, kreverToTrinn: Boolean?) {
        val avklaringsbehov = hent(behandlingId).hentBehovForDefinisjon(definisjon)

        if (avklaringsbehov == null) {
            throw IllegalStateException("Fant ikke avklaringsbehov med $definisjon for behandling $behandlingId")
        }

        if (kreverToTrinn != null) {
            kreverToTrinn(avklaringsbehov.id, kreverToTrinn)
        }

        opprettAvklaringsbehovEndring(avklaringsbehov.id, Status.AVSLUTTET, begrunnelse, "Saksbehandler")
    }

    override fun opprettAvklaringsbehovEndring(
        avklaringsbehovId: Long,
        status: Status,
        begrunnelse: String,
        opprettetAv: String
    ) {
        val query = """
            INSERT INTO AVKLARINGSBEHOV_ENDRING (avklaringsbehov_id, status, begrunnelse, opprettet_av, opprettet_tid) 
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, avklaringsbehovId)
                setEnumName(2, status)
                setString(3, begrunnelse)
                setString(4, opprettetAv)
                setLocalDateTime(5, LocalDateTime.now())
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): Avklaringsbehovene {
        return Avklaringsbehovene(
            repository = this,
            behandlingId = behandlingId
        )
    }

    override fun hentBehovene(behandlingId: BehandlingId): List<Avklaringsbehov> {
        val query = """
            SELECT * FROM AVKLARINGSBEHOV WHERE behandling_id = ?
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                mapAvklaringsbehov(it)
            }
        }
    }

    override fun kreverToTrinn(avklaringsbehovId: Long, kreverToTrinn: Boolean) {
        val query = """
            UPDATE AVKLARINGSBEHOV SET krever_to_trinn = ? WHERE id = ?
            """.trimIndent()

        connection.execute(query) {
            setParams {
                setBoolean(1, kreverToTrinn)
                setLong(2, avklaringsbehovId)
            }
        }
    }

    private fun mapAvklaringsbehov(row: Row): Avklaringsbehov {
        val definisjon = Definisjon.forKode(row.getString("definisjon"))
        val id = row.getLong("id")
        return Avklaringsbehov(
            id = id,
            definisjon = definisjon,
            funnetISteg = row.getEnum("funnet_i_steg"),
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
            status = row.getEnum("status"),
            tidsstempel = row.getLocalDateTime("opprettet_tid"),
            begrunnelse = row.getString("begrunnelse"),
            endretAv = row.getString("opprettet_av")
        )
    }
}
