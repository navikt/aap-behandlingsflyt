package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.ForhåndsvarselKlageFormkrav
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate

class EffektuerAvvistPåFormkravRepositoryImpl(private val connection: DBConnection) :
    EffektuerAvvistPåFormkravRepository {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object : Factory<EffektuerAvvistPåFormkravRepositoryImpl> {
        override fun konstruer(connection: DBConnection): EffektuerAvvistPåFormkravRepositoryImpl {
            return EffektuerAvvistPåFormkravRepositoryImpl(connection)
        }
    }
    
    override fun lagreVarsel(
        behandlingId: BehandlingId,
        varsel: BrevbestillingReferanse
    ) {
        
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            throw IllegalStateException(
                "Kan ikke lagre varsel for behandling $behandlingId, da det allerede finnes et grunnlag for denne behandlingen."
            )
        }

        val varselId = connection.executeReturnKey(
            "insert into avvist_formkrav_varsel(brev_referanse) values(?)"
        ) {
            setParams {
                setUUID(1, varsel.brevbestillingReferanse)
            }
        }

        connection.execute(
            "insert into effektuer_avvist_formkrav_grunnlag (behandling_id, varsel_id) values (?, ?)"
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, varselId)
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): EffektuerAvvistPåFormkravGrunnlag? {
        val query = """
            select grunnlag.varsel_id, varsel.dato_varslet, varsel.frist, varsel.brev_referanse, vurdering.skal_endelig_avvises
            from effektuer_avvist_formkrav_grunnlag grunnlag
            left join avvist_formkrav_varsel varsel on grunnlag.varsel_id = varsel.id
            left join effektuer_avvist_formkrav_vurdering vurdering on grunnlag.vurdering_id = vurdering.id
            where grunnlag.aktiv and grunnlag.behandling_id = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper(::mapGrunnlag)
        }
    }
    
    override fun lagreVurdering(
        behandlingId: BehandlingId,
        vurdering: EffektuerAvvistPåFormkravVurdering
    ) {
        val gjeldendeGrunnlag = deaktiverGjeldendeGrunnlag(behandlingId)
            ?: throw IllegalStateException("Ingen aktivt grunnlag for behandling $behandlingId")

        val vurderingId = connection.executeReturnKey(
            "insert into effektuer_avvist_formkrav_vurdering(skal_endelig_avvises) values(?)"
        ) {
            setParams { setBoolean(1, vurdering.skalEndeligAvvises) }
        }

        connection.execute(
            """insert into effektuer_avvist_formkrav_grunnlag (behandling_id, vurdering_id, varsel_id)
                select ?, ?, varsel_id from effektuer_avvist_formkrav_grunnlag where id = ?
            """.trimMargin()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingId)
                setLong(3, gjeldendeGrunnlag)
            }
        }
    }

    override fun lagreFrist(
        behandlingId: BehandlingId,
        datoVarslet: LocalDate,
        frist: LocalDate
    ) {
        val gjeldendeGrunnlag = deaktiverGjeldendeGrunnlag(behandlingId)
            ?: throw IllegalStateException("Ingen aktivt grunnlag for behandling $behandlingId")

        val varselId = connection.executeReturnKey(
            """insert into avvist_formkrav_varsel (frist, dato_varslet, brev_referanse)
                select ?, ?, brev_referanse from avvist_formkrav_varsel where id in (
                    select varsel_id from effektuer_avvist_formkrav_grunnlag where id = ? limit 1
                )
            """.trimMargin()
        ) {
            setParams {
                setLocalDate(1, frist)
                setLocalDate(2, datoVarslet)
                setLong(3, gjeldendeGrunnlag)
            }
        }

        connection.execute(
            "insert into effektuer_avvist_formkrav_grunnlag (behandling_id, varsel_id) values (?, ?)"
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, varselId)
            }
        }
        
    }

    private fun mapGrunnlag(row: Row): EffektuerAvvistPåFormkravGrunnlag {
        return EffektuerAvvistPåFormkravGrunnlag(
            varsel = mapVarsel(row),
            vurdering = mapVurdering(row)
        )
    }

    private fun mapVarsel(row: Row): ForhåndsvarselKlageFormkrav {
        return ForhåndsvarselKlageFormkrav(
            datoVarslet = row.getLocalDateOrNull("dato_varslet"),
            frist = row.getLocalDateOrNull("frist"),
            referanse = BrevbestillingReferanse(
                brevbestillingReferanse = row.getUUID("brev_referanse")
            )
        )
    }
    
    private fun mapVurdering(row: Row): EffektuerAvvistPåFormkravVurdering? {
        return row.getBooleanOrNull("skal_endelig_avvises")?.let { skalEndeligAvvises ->
            EffektuerAvvistPåFormkravVurdering(skalEndeligAvvises)
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        // Do nothing
    }

    override fun slett(behandlingId: BehandlingId) {
        val vurderingIds = getIdForVurderingerForGrunnlaget(behandlingId)

        val deletedRowsGrunnlag = connection.executeReturnUpdated("""
            delete from effektuer_avvist_formkrav_grunnlag where behandling_id = ?; 
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
            }
        }

        val deletedRowsVurdering = connection.executeReturnUpdated("""
            delete from effektuer_avvist_formkrav_vurdering where id = ANY(?::bigint[]);
        """.trimIndent()) {
            setParams {
                setLongArray(1, vurderingIds)
            }
        }

        log.info("Slettet $deletedRowsGrunnlag rader fra effektuer_avvist_formkrav_grunnlag og $deletedRowsVurdering rader fra effektuer_avvist_formkrav_vurdering")
    }

    private fun getIdForVurderingerForGrunnlaget(behandlingId: BehandlingId): List<Long> =
        connection.queryList(
            """
                SELECT vurdering_id
                FROM effektuer_avvist_formkrav_grunnlag
                WHERE behandling_id = ? AND vurdering_id is not null
                """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row ->
                row.getLong("vurdering_id")
            }
        }

    private fun deaktiverGjeldendeGrunnlag(behandlingId: BehandlingId): Long? {
        return connection.queryFirstOrNull(
            """
            update effektuer_avvist_formkrav_grunnlag
            set aktiv = false
            where aktiv and behandling_id = ? returning id
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper {
                it.getLongOrNull("id")
            }
        }
    }
}