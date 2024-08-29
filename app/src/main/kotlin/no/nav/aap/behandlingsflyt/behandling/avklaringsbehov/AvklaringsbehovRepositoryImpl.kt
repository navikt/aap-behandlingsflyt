package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate
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

    override fun opprett(
        behandlingId: BehandlingId,
        definisjon: Definisjon,
        funnetISteg: StegType,
        frist: LocalDate?,
        begrunnelse: String,
        grunn: ÅrsakTilSettPåVent?,
        endretAv: String
    ) {
        var avklaringsbehovId = hentRelevantAvklaringsbehov(behandlingId, definisjon)

        if (avklaringsbehovId == null) {
            avklaringsbehovId = opprettAvklaringsbehov(behandlingId, definisjon, funnetISteg)
        }

        endreAvklaringsbehov(
            avklaringsbehovId,
            Endring(
                status = Status.OPPRETTET,
                begrunnelse = begrunnelse,
                grunn = grunn,
                endretAv = endretAv,
                frist = frist
            )
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

    override fun endre(avklaringsbehovId: Long, endring: Endring) {
        endreAvklaringsbehov(
            avklaringsbehovId,
            endring
        )
    }

    override fun endreVentepunkt(avklaringsbehovId: Long, endring: Endring, funnetISteg: StegType) {
        oppdaterFunnetISteg(avklaringsbehovId, funnetISteg)
        endreAvklaringsbehov(
            avklaringsbehovId,
            endring
        )
    }

    private fun oppdaterFunnetISteg(avklaringsbehovId: Long, funnetISteg: StegType) {
        val query = """
                    UPDATE AVKLARINGSBEHOV 
                    SET funnet_i_steg = ? 
                    WHERE id = ?
                    """.trimIndent()

        connection.execute(query) {
            setParams {
                setEnumName(1, funnetISteg)
                setLong(2, avklaringsbehovId)
            }
        }
    }

    private fun endreAvklaringsbehov(
        avklaringsbehovId: Long,
        endring: Endring
    ) {
        val query = """
            INSERT INTO AVKLARINGSBEHOV_ENDRING (avklaringsbehov_id, status, begrunnelse, frist, opprettet_av, opprettet_tid, venteaarsak) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        val opprettetAv = endring.endretAv

        val key = connection.executeReturnKey(query) {
            setParams {
                setLong(1, avklaringsbehovId)
                setEnumName(2, endring.status)
                setString(3, endring.begrunnelse)
                setLocalDate(4, endring.frist)
                setString(5, opprettetAv)
                setLocalDateTime(6, LocalDateTime.now())
                setEnumName(7, endring.grunn)
            }
        }
        val queryPeriode = """
                    INSERT INTO AVKLARINGSBEHOV_ENDRING_AARSAK (endring_id, aarsak_til_retur, aarsak_til_retur_fritekst, OPPRETTET_AV) VALUES (?, ?, ?, ?)
                """.trimIndent()
        connection.executeBatch(queryPeriode, endring.årsakTilRetur) {
            setParams {
                setLong(1, key)
                setEnumName(2, it.årsak)
                setString(3, it.årsakFritekst)
                setString(4, opprettetAv)
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): List<no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov> {
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

    private fun mapAvklaringsbehov(row: Row): no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov {
        val definisjon = Definisjon.forKode(row.getString("definisjon"))
        val id = row.getLong("id")
        return no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov(
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
            frist = row.getLocalDateOrNull("frist"),
            årsakTilRetur = hentÅrsaker(row.getLong("id")),
            grunn = row.getEnumOrNull("venteaarsak")
        )
    }

    private fun hentÅrsaker(endringId: Long): List<ÅrsakTilRetur> {
        val query = """
            SELECT * FROM AVKLARINGSBEHOV_ENDRING_AARSAK 
            WHERE endring_id = ? 
            ORDER BY opprettet_tid ASC
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, endringId)
            }
            setRowMapper {
                mapÅrsaker(it)
            }
        }
    }

    private fun mapÅrsaker(row: Row): ÅrsakTilRetur {
        return ÅrsakTilRetur(row.getEnum("aarsak_til_retur"), row.getStringOrNull("aarsak_til_retur_fritekst"))
    }
}
