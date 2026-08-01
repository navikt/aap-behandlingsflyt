package no.nav.aap.behandlingsflyt.repository.avklaringsbehov

import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovForSak
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovOperasjonerRepository
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.ÅrsakTilReturKode
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
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
        endretAv: Bruker,
        perioderSomIkkeErTilstrekkeligVurdert: Set<Periode>?,
        perioderVedtaketBehøverVurdering: Set<Periode>?
    ) {
        val avklaringsbehovId = finnEllerOpprettAvklaringsbehov(
            behandlingId,
            definisjon,
            funnetISteg
        )

        endreAvklaringsbehov(
            avklaringsbehovId,
            Endring(
                status = Status.OPPRETTET,
                begrunnelse = begrunnelse,
                grunn = grunn,
                endretAv = endretAv,
                frist = frist,
                perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert,
                perioderVedtaketBehøverVurdering = perioderVedtaketBehøverVurdering
            )
        )
    }

    override fun slett(behandlingId: BehandlingId) {
        // Det kan ikke avgjøres her hvilke avklaringsbehov som skal slettes og ikke, så det ansvaret overlates til hvert steg,
        // og gjøres som en del av utfør-metoden i det enkelte steg
    }

    private fun finnEllerOpprettAvklaringsbehov(
        behandlingId: BehandlingId,
        definisjon: Definisjon,
        funnetISteg: StegType
    ): Long {
        val query = """
            WITH inserted AS (
                INSERT INTO AVKLARINGSBEHOV (behandling_id, definisjon, funnet_i_steg) 
                VALUES (?, ?, ?)
                ON CONFLICT (behandling_id, definisjon) DO NOTHING
                RETURNING id
            )
            SELECT id FROM inserted
            UNION ALL
            SELECT id FROM AVKLARINGSBEHOV WHERE behandling_id = ? AND definisjon = ?
            LIMIT 1
        """.trimIndent()

        return checkNotNull(connection.queryFirstOrNull<Long>(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, definisjon.kode)
                setEnumName(3, funnetISteg)
                setLong(4, behandlingId.toLong())
                setEnumName(5, definisjon.kode)
            }
            setRowMapper {
                it.getLong("id")
            }
        }) { "Finner ikke avklaringsbehov for behandling=${behandlingId.id}, definisjon=${definisjon.kode}" }
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
            INSERT INTO AVKLARINGSBEHOV_ENDRING (avklaringsbehov_id, status, begrunnelse, frist, opprettet_av, opprettet_tid, venteaarsak, perioder_ugyldig_vurdering, perioder_krever_vurdering) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

        val opprettetAv = endring.endretAv

        val key = connection.executeReturnKey(query) {
            setParams {
                setLong(1, avklaringsbehovId)
                setEnumName(2, endring.status)
                setString(3, endring.begrunnelse)
                setLocalDate(4, endring.frist)
                setBruker(5, opprettetAv)
                setLocalDateTime(6, LocalDateTime.now())
                setEnumName(7, endring.grunn)
                setPeriodeArray(8, endring.perioderSomIkkeErTilstrekkeligVurdert?.toList())
                setPeriodeArray(9, endring.perioderVedtaketBehøverVurdering?.toList())
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
                setBruker(4, opprettetAv)
            }
        }
    }

    override fun hentAlleAvklaringsbehovForSak(behandlingIder: List<BehandlingId>): List<AvklaringsbehovForSak> {
        if (behandlingIder.isEmpty()) return emptyList()

        val query = """
            SELECT
                ab.id AS ab_id,
                ab.definisjon AS ab_definisjon,
                ab.funnet_i_steg AS ab_funnet_i_steg,
                ab.krever_to_trinn AS ab_krever_to_trinn,
                ab.behandling_id AS ab_behandling_id,
                ae.id AS endring_id,
                ae.avklaringsbehov_id AS endring_avklaringsbehov_id,
                ae.status AS endring_status,
                ae.opprettet_tid AS endring_opprettet_tid,
                ae.begrunnelse AS endring_begrunnelse,
                ae.opprettet_av AS endring_opprettet_av,
                ae.frist AS endring_frist,
                ae.venteaarsak AS endring_venteaarsak,
                ae.perioder_ugyldig_vurdering AS endring_perioder_ugyldig_vurdering,
                ae.perioder_krever_vurdering AS endring_perioder_krever_vurdering,
                aea.endring_id AS retur_endring_id,
                aea.aarsak_til_retur AS retur_aarsak,
                aea.aarsak_til_retur_fritekst AS retur_aarsak_fritekst
            FROM AVKLARINGSBEHOV ab
            LEFT JOIN AVKLARINGSBEHOV_ENDRING ae ON ae.avklaringsbehov_id = ab.id
            LEFT JOIN AVKLARINGSBEHOV_ENDRING_AARSAK aea ON aea.endring_id = ae.id
            WHERE ab.behandling_id = ANY(?::bigint[])
            ORDER BY ab.id, ae.id, aea.id
        """.trimIndent()

        val rader = connection.queryList(query) {
            setParams {
                setArray(1, behandlingIder.map { "${it.id}" })
            }
            setRowMapper { mapRad(it) }
        }

        val raderPerBehandling = rader.groupBy { it.avklaringsbehov.behandlingId }

        return behandlingIder.map { behandlingId ->
            val behovForBehandling = mapTilAvklaringsbehov(raderPerBehandling[behandlingId.toLong()].orEmpty())
            AvklaringsbehovForSak(behandlingId, behovForBehandling)
        }
    }


    override fun hent(behandlingId: BehandlingId): List<Avklaringsbehov> {
        val query = """
            SELECT
                ab.id AS ab_id,
                ab.definisjon AS ab_definisjon,
                ab.funnet_i_steg AS ab_funnet_i_steg,
                ab.krever_to_trinn AS ab_krever_to_trinn,
                ab.behandling_id AS ab_behandling_id,
                ae.id AS endring_id,
                ae.avklaringsbehov_id AS endring_avklaringsbehov_id,
                ae.status AS endring_status,
                ae.opprettet_tid AS endring_opprettet_tid,
                ae.begrunnelse AS endring_begrunnelse,
                ae.opprettet_av AS endring_opprettet_av,
                ae.frist AS endring_frist,
                ae.venteaarsak AS endring_venteaarsak,
                ae.perioder_ugyldig_vurdering AS endring_perioder_ugyldig_vurdering,
                ae.perioder_krever_vurdering AS endring_perioder_krever_vurdering,
                aea.endring_id AS retur_endring_id,
                aea.aarsak_til_retur AS retur_aarsak,
                aea.aarsak_til_retur_fritekst AS retur_aarsak_fritekst
            FROM AVKLARINGSBEHOV ab
            LEFT JOIN AVKLARINGSBEHOV_ENDRING ae ON ae.avklaringsbehov_id = ab.id
            LEFT JOIN AVKLARINGSBEHOV_ENDRING_AARSAK aea ON aea.endring_id = ae.id
            WHERE ab.behandling_id = ?
            ORDER BY ab.id, ae.id, aea.id
        """.trimIndent()

        val rader = connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { mapRad(it) }
        }

        return mapTilAvklaringsbehov(rader)
    }

    private fun mapTilAvklaringsbehov(rader: List<AvklaringsbehovRad>): List<Avklaringsbehov> {
        if (rader.isEmpty()) return emptyList()
        return rader
            .groupBy { it.avklaringsbehov.id }
            .values
            .map { raderForAvklaringsbehov ->
                mapTilAvklaringsBehov(
                    raderForAvklaringsbehov.first().avklaringsbehov,
                    raderForAvklaringsbehov
                )
            }
    }

    private fun mapTilAvklaringsBehov(
        avklaringsbehov: AvklaringsbehovInternal,
        raderForAvklaringsbehov: List<AvklaringsbehovRad>
    ): Avklaringsbehov {
        val årsakerPerEndring = raderForAvklaringsbehov
            .mapNotNull { it.årsak }
            .distinctBy { Triple(it.endringId, it.årsak, it.årsakFritekst) }
            .groupBy { it.endringId }

        val relevanteEndringer = raderForAvklaringsbehov
            .mapNotNull { it.endring }
            .associateBy { it.id }
            .values
            .map { endring -> mapEndring(endring, årsakerPerEndring[endring.id].orEmpty()) }
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

    private fun mapRad(row: Row): AvklaringsbehovRad {
        val avklaringsbehov = AvklaringsbehovInternal(
            id = row.getLong("ab_id"),
            definisjon = Definisjon.forKode(row.getEnum<AvklaringsbehovKode>("ab_definisjon")),
            funnetISteg = row.getEnum("ab_funnet_i_steg"),
            kreverToTrinn = row.getBooleanOrNull("ab_krever_to_trinn"),
            behandlingId = row.getLong("ab_behandling_id")
        )

        val endringId = row.getLongOrNull("endring_id")
        val endring = endringId?.let {
            EndringInternal(
                id = it,
                avklaringsbehovId = row.getLong("endring_avklaringsbehov_id"),
                status = row.getEnum("endring_status"),
                tidsstempel = row.getLocalDateTime("endring_opprettet_tid"),
                begrunnelse = row.getString("endring_begrunnelse"),
                endretAv = row.getBruker("endring_opprettet_av"),
                frist = row.getLocalDateOrNull("endring_frist"),
                grunn = row.getEnumOrNull("endring_venteaarsak"),
                perioderSomIkkeErTilstrekkeligVurdert = row.getPeriodeArrayOrNull("endring_perioder_ugyldig_vurdering")
                    ?.toSet(),
                perioderVedtaketBehøverVurdering = row.getPeriodeArrayOrNull("endring_perioder_krever_vurdering")
                    ?.toSet()
            )
        }

        val årsak = row.getLongOrNull("retur_endring_id")?.let {
            ÅrsakInternal(
                endringId = it,
                årsak = row.getEnum("retur_aarsak"),
                årsakFritekst = row.getStringOrNull("retur_aarsak_fritekst")
            )
        }

        return AvklaringsbehovRad(
            avklaringsbehov = avklaringsbehov,
            endring = endring,
            årsak = årsak
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
            perioderSomIkkeErTilstrekkeligVurdert = endring.perioderSomIkkeErTilstrekkeligVurdert,
            perioderVedtaketBehøverVurdering = endring.perioderVedtaketBehøverVurdering
        )
    }

    internal data class AvklaringsbehovInternal(
        val id: Long,
        val definisjon: Definisjon,
        val funnetISteg: StegType,
        val kreverToTrinn: Boolean?,
        val behandlingId: Long
    )

    internal data class EndringInternal(
        val id: Long,
        val avklaringsbehovId: Long,
        val status: Status,
        val tidsstempel: LocalDateTime,
        val begrunnelse: String,
        val endretAv: Bruker,
        val frist: LocalDate?,
        val grunn: ÅrsakTilSettPåVent?,
        val perioderSomIkkeErTilstrekkeligVurdert: Set<Periode>?,
        val perioderVedtaketBehøverVurdering: Set<Periode>?
    )

    internal data class ÅrsakInternal(val endringId: Long, val årsak: ÅrsakTilReturKode, val årsakFritekst: String?)

    internal data class AvklaringsbehovRad(
        val avklaringsbehov: AvklaringsbehovInternal,
        val endring: EndringInternal?,
        val årsak: ÅrsakInternal?
    )

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Denne trengs ikke implementeres
    }
}