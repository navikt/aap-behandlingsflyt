package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDateTime

class AvklaringsbehovRepositoryImpl(private val connection: DBConnection) : AvklaringsbehovRepository,
    AvklaringsbehovOperasjonerRepository {

    override fun hentAvklaringsbehovene(behandlingId: BehandlingId): Avklaringsbehovene {
        return Avklaringsbehovene(
            repository = this,
            behandlingId = behandlingId
        )
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

    override fun opprett(behandlingId: BehandlingId, definisjon: Definisjon, funnetISteg: StegType) {
        //TODO: Kan vi utelukke denne sjekken? LeggTil burde alltid opprette - finnes den fra før må den evt. endres.
        var avklaringsbehovId = hentRelevantAvklaringsbehov(behandlingId, definisjon)

        if (avklaringsbehovId == null) {
            avklaringsbehovId = opprettAvklaringsbehov(behandlingId, definisjon, funnetISteg)
        }

        endreAvklaringsbehov(
            avklaringsbehovId,
            Status.OPPRETTET,
            "",
            SYSTEMBRUKER.ident,
            null,
            null
        )
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

    //TODO: Hvorfor setter vi ikke toTrinn som true/false når vi oppretter et Avklaringsbehov? Hvorfor er dette nullable?
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

    override fun endre(avklaringsbehov: Avklaringsbehov) {
        endreAvklaringsbehov(
            avklaringsbehov.id,
            avklaringsbehov.status(),
            avklaringsbehov.begrunnelse(),
            avklaringsbehov.endretAv(),
            avklaringsbehov.årsakTilRetur(),
            avklaringsbehov.årsakTilReturFritekst()
        )
    }

    private fun endreAvklaringsbehov(
        avklaringsbehovId: Long,
        status: Status,
        begrunnelse: String,
        opprettetAv: String,
        årsakTilRetur: ÅrsakTilRetur?,
        årsakTilReturFritekst: String?
    ) {
        val query = """
            INSERT INTO AVKLARINGSBEHOV_ENDRING (avklaringsbehov_id, status, begrunnelse, aarsak_til_retur, aarsak_til_retur_fritekst, opprettet_av, opprettet_tid) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, avklaringsbehovId)
                setEnumName(2, status)
                setString(3, begrunnelse)
                setEnumName(4, årsakTilRetur)
                setString(5, årsakTilReturFritekst)
                setString(6, opprettetAv)
                setLocalDateTime(7, LocalDateTime.now())
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): List<Avklaringsbehov> {
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
            endretAv = row.getString("opprettet_av"),
            årsakTilRetur = row.getEnumOrNull("aarsak_til_retur"),
            årsakTilReturFritekst = row.getStringOrNull("aarsak_til_retur_fritekst")
        )
    }
}
