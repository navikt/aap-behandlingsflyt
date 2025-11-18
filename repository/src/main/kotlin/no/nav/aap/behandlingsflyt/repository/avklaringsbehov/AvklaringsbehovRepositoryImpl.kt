package no.nav.aap.behandlingsflyt.repository.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovForSak
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOperasjonerRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.ÅrsakTilReturKode
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import java.time.LocalDate
import java.time.LocalDateTime

class AvklaringsbehovRepositoryImpl(private val connection: DBConnection) : AvklaringsbehovRepository,
    AvklaringsbehovOperasjonerRepository {

    companion object : Factory<AvklaringsbehovRepositoryImpl> {
        override fun konstruer(connection: DBConnection): AvklaringsbehovRepositoryImpl {
            return AvklaringsbehovRepositoryImpl(connection)
        }
    }

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
        endretAv: String,
        perioderSomIkkeErTilstrekkeligVurdert: Set<Periode>?,
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
                frist = frist,
                perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert
            )
        )
    }

    override fun slett(behandlingId: BehandlingId) {
        // Det kan ikke avgjøres her hvilke avklaringsbehov som skal slettes og ikke, så det ansvaret overlates til hvert steg,
        // og gjøres som en del av utfør-metoden i det enkelte steg
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
                setEnumName(2, definisjon.kode)
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
                setEnumName(2, definisjon.kode)
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

    override fun endreSkrivBrev(
        avklaringsbehovId: Long, endring: Endring, funnetISteg: StegType
    ) {
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
            INSERT INTO AVKLARINGSBEHOV_ENDRING (avklaringsbehov_id, status, begrunnelse, frist, opprettet_av, opprettet_tid, venteaarsak, perioder) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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
                setPeriodeArray(8, endring.perioderSomIkkeErTilstrekkeligVurdert?.toList())
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

    override fun hentAlleAvklaringsbehovForSak(behandlingIder: List<BehandlingId>): List<AvklaringsbehovForSak> {
        if (behandlingIder.isEmpty()) return emptyList()

        val avklaringsbehovQuery = """
        SELECT * 
        FROM AVKLARINGSBEHOV ab
        WHERE behandling_id = ANY(?::bigint[])
    """.trimIndent()

        val avklaringsbehovInternal = connection.queryList(avklaringsbehovQuery) {
            setParams {
                setArray(1, behandlingIder.map { "${it.id}" })
            }
            setRowMapper { mapAvklaringsbehov(it) }
        }

        val endringerQuery = """
        SELECT * 
        FROM AVKLARINGSBEHOV_ENDRING 
        WHERE avklaringsbehov_id = ANY(?::bigint[])
    """.trimIndent()

        val endringerInternal = connection.queryList(endringerQuery) {
            setParams {
                setArray(1, avklaringsbehovInternal.map { "${it.id}" })
            }
            setRowMapper { mapEndringer(it) }
        }

        val årsakerInternal = if (endringerInternal.isNotEmpty()) {
            val årsakerQuery = """
            SELECT * 
            FROM AVKLARINGSBEHOV_ENDRING_AARSAK 
            WHERE endring_id = ANY(?::bigint[])
        """.trimIndent()

            connection.queryList(årsakerQuery) {
                setParams {
                    setArray(1, endringerInternal.map { "${it.id}" })
                }
                setRowMapper { mapÅrsaker(it) }
            }
        } else {
            emptyList()
        }

        val avklaringsbehovByBehandling = avklaringsbehovInternal.groupBy { it.behandlingId }

        return behandlingIder.map { behandlingId ->
            val behovForBehandling = avklaringsbehovByBehandling[behandlingId.toLong()].orEmpty().map { behov ->
                mapTilAvklaringsBehov(
                    behov,
                    endringerInternal,
                    årsakerInternal
                )
            }
            AvklaringsbehovForSak(behandlingId, behovForBehandling)
        }
    }


    override fun hent(behandlingId: BehandlingId): List<Avklaringsbehov> {
        val avklaringsbehovQuery = """
            SELECT * 
            FROM AVKLARINGSBEHOV ab
            WHERE behandling_id = ?
            """.trimIndent()

        val avklaringsbehovInternal = connection.queryList(avklaringsbehovQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                mapAvklaringsbehov(it)
            }
        }

        val endringerQuery = """
            SELECT * FROM AVKLARINGSBEHOV_ENDRING 
            WHERE avklaringsbehov_id = ANY(?::bigint[])
            """.trimIndent()
        val endringerInternal = connection.queryList(endringerQuery) {
            setParams {
                setArray(1, avklaringsbehovInternal.map { "${it.id}" })
            }
            setRowMapper {
                mapEndringer(it)
            }
        }

        val årsakerQuery = """
            SELECT * FROM AVKLARINGSBEHOV_ENDRING_AARSAK 
            WHERE endring_id = ANY(?::bigint[])
        """.trimIndent()

        val årsakerInternal = connection.queryList(årsakerQuery) {
            setParams {
                setArray(1, endringerInternal.map { "${it.id}" })
            }
            setRowMapper {
                mapÅrsaker(it)
            }
        }

        return avklaringsbehovInternal.map { mapTilAvklaringsBehov(it, endringerInternal, årsakerInternal) }
    }

    private fun mapTilAvklaringsBehov(
        avklaringsbehov: AvklaringsbehovInternal,
        endringer: List<EndringInternal>,
        årsaker: List<ÅrsakInternal>
    ): Avklaringsbehov {

        val relevanteEndringer = endringer
            .filter { it.avklaringsbehovId == avklaringsbehov.id }
            .map { endring -> mapEndring(endring, årsaker) }
            .sorted()
            .toMutableList()

        return Avklaringsbehov(
            id = avklaringsbehov.id,
            definisjon = avklaringsbehov.definisjon,
            historikk = relevanteEndringer,
            funnetISteg = avklaringsbehov.funnetISteg,
            kreverToTrinn = avklaringsbehov.kreverToTrinn
        )
    }

    private fun mapEndring(
        endring: EndringInternal,
        årsaker: List<ÅrsakInternal>
    ): Endring {
        val relevanteÅrsaker = årsaker
            .filter { it.endringId == endring.id }
            .map { årsak -> ÅrsakTilRetur(årsak = årsak.årsak, årsakFritekst = årsak.årsakFritekst) }

        return Endring(
            status = endring.status,
            tidsstempel = endring.tidsstempel,
            begrunnelse = endring.begrunnelse,
            grunn = endring.grunn,
            frist = endring.frist,
            endretAv = endring.endretAv,
            årsakTilRetur = relevanteÅrsaker,
            perioderSomIkkeErTilstrekkeligVurdert = endring.perioderSomIkkeErTilstrekkeligVurdert
        )
    }

    private fun mapAvklaringsbehov(row: Row): AvklaringsbehovInternal {
        val definisjon = Definisjon.Companion.forKode(row.getEnum<AvklaringsbehovKode>("definisjon"))
        val id = row.getLong("id")
        return AvklaringsbehovInternal(
            id = id,
            definisjon = definisjon,
            funnetISteg = row.getEnum("funnet_i_steg"),
            kreverToTrinn = row.getBooleanOrNull("krever_to_trinn"),
            behandlingId = row.getLong("behandling_id"),

            )
    }


    private fun mapEndringer(row: Row): EndringInternal {
        return EndringInternal(
            id = row.getLong("id"),
            avklaringsbehovId = row.getLong("avklaringsbehov_id"),
            status = row.getEnum("status"),
            tidsstempel = row.getLocalDateTime("opprettet_tid"),
            begrunnelse = row.getString("begrunnelse"),
            endretAv = row.getString("opprettet_av"),
            frist = row.getLocalDateOrNull("frist"),
            grunn = row.getEnumOrNull("venteaarsak"),
            perioderSomIkkeErTilstrekkeligVurdert = row.getPeriodeArrayOrNull("perioder")?.toSet()
        )
    }

    private fun mapÅrsaker(row: Row): ÅrsakInternal {
        return ÅrsakInternal(
            årsak = row.getEnum("aarsak_til_retur"),
            endringId = row.getLong("endring_id"),
            årsakFritekst = row.getStringOrNull("aarsak_til_retur_fritekst")
        )
    }

    internal class AvklaringsbehovInternal(
        val id: Long,
        val definisjon: Definisjon,
        val funnetISteg: StegType,
        val kreverToTrinn: Boolean?,
        val behandlingId: Long?
    )

    internal class EndringInternal(
        val id: Long,
        val avklaringsbehovId: Long,
        val status: Status,
        val tidsstempel: LocalDateTime,
        val begrunnelse: String,
        val endretAv: String,
        val frist: LocalDate?,
        val grunn: ÅrsakTilSettPåVent?,
        val perioderSomIkkeErTilstrekkeligVurdert: Set<Periode>?
    )

    internal class ÅrsakInternal(val endringId: Long, val årsak: ÅrsakTilReturKode, val årsakFritekst: String?)

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Denne trengs ikke implementeres
    }
}