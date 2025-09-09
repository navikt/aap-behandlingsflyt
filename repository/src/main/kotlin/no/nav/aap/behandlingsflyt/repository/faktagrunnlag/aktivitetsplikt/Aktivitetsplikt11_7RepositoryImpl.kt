package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Varsel
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate

class Aktivitetsplikt11_7RepositoryImpl(private val connection: DBConnection) : Aktivitetsplikt11_7Repository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<Aktivitetsplikt11_7RepositoryImpl> {
        override fun konstruer(connection: DBConnection): Aktivitetsplikt11_7RepositoryImpl {
            return Aktivitetsplikt11_7RepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Aktivitetsplikt11_7Grunnlag? {
        val query = """
            select v.* 
            from aktivitetsplikt_11_7_grunnlag g
            inner join aktivitetsplikt_11_7_vurderinger vs on g.vurderinger_id = vs.id
            inner join aktivitetsplikt_11_7_vurdering v  on vs.id = v.vurderinger_id
            where g.aktiv = true and g.behandling_id = ?
        """.trimIndent()

        val vurderinger = connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapVurdering)
        }
        return if (vurderinger.isEmpty()) {
            null
        } else {
            Aktivitetsplikt11_7Grunnlag(vurderinger = vurderinger)
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vurderinger: List<Aktivitetsplikt11_7Vurdering>
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = Aktivitetsplikt11_7Grunnlag(vurderinger = vurderinger)

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    override fun lagreVarsel(
        behandlingId: BehandlingId,
        varsel: BrevbestillingReferanse
    ) {
        val query = """
            INSERT INTO aktivitetsplikt_11_7_varsel (behandling_id, brev_referanse) 
            VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setUUID(2, varsel.brevbestillingReferanse)
            }
        }
    }

    override fun lagreFrist(
        behandlingId: BehandlingId,
        datoVarslet: LocalDate,
        svarfrist: LocalDate
    ) {
        val query = """
            UPDATE aktivitetsplikt_11_7_varsel SET dato_varslet=?, frist=? WHERE behandling_id=?
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLocalDate(1, datoVarslet)
                setLocalDate(2, svarfrist)
                setLong(3, behandlingId.toLong())
            }
        }
    }

    override fun hentVarselHvisEksisterer(behandlingId: BehandlingId): Aktivitetsplikt11_7Varsel? {
        val query = """
            select * 
            from aktivitetsplikt_11_7_varsel
            where behandling_id = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapVarsel)
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        val query = """
            insert into aktivitetsplikt_11_7_grunnlag (behandling_id, vurderinger_id, aktiv)
            select ?, vurderinger_id, true
            from aktivitetsplikt_11_7_grunnlag
            where behandling_id = ? and aktiv
        """.trimIndent()
        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        // TODO: Avgjør om vi trenger dette. Gjør ingenting inntil videre
        log.warn("Forsøkte å slette aktivitetsplikt-grunnlag, men sletting er ikke implementert")
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: Aktivitetsplikt11_7Grunnlag) {
        val vurderingerId = lagreVurderinger(nyttGrunnlag.vurderinger)
        val query = """
            insert into aktivitetsplikt_11_7_grunnlag 
            (behandling_id, vurderinger_id, aktiv) 
            values (?, ?, true)
        """.trimIndent()
        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingerId)
            }
        }
    }

    private fun lagreVurderinger(vurderinger: List<Aktivitetsplikt11_7Vurdering>): Long {
        val vurderingerId = connection.executeReturnKey(
            """
            insert into aktivitetsplikt_11_7_vurderinger default values
        """.trimIndent()
        )

        val query = """
            INSERT INTO aktivitetsplikt_11_7_vurdering 
            (begrunnelse, er_oppfylt, utfall, vurdert_av, vurderingen_gjelder_fra, opprettet_tid, vurderinger_id, vurdert_i_behandling, skal_ignorere_varsel_frist) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(query, vurderinger) {
            setParams { vurdering ->
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erOppfylt)
                setEnumName(3, vurdering.utfall)
                setString(4, vurdering.vurdertAv)
                setLocalDate(5, vurdering.gjelderFra)
                setInstant(6, vurdering.opprettet)
                setLong(7, vurderingerId)
                setLong(8, vurdering.vurdertIBehandling.toLong())
                setBoolean(9, vurdering.skalIgnorereVarselFrist)
            }
        }

        return vurderingerId
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE AKTIVITETSPLIKT_11_7_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    private fun mapVurdering(row: Row): Aktivitetsplikt11_7Vurdering {
        return Aktivitetsplikt11_7Vurdering(
            begrunnelse = row.getString("begrunnelse"),
            erOppfylt = row.getBoolean("er_oppfylt"),
            utfall = row.getStringOrNull("utfall")?.let { Utfall.valueOf(it) },
            vurdertAv = row.getString("vurdert_av"),
            gjelderFra = row.getLocalDate("vurderingen_gjelder_fra"),
            opprettet = row.getInstant("opprettet_tid"),
            vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
            skalIgnorereVarselFrist = row.getBoolean("skal_ignorere_varsel_frist"),
        )
    }

    private fun mapVarsel(row: Row): Aktivitetsplikt11_7Varsel? {
        val varselUuid = row.getUUIDOrNull("brev_referanse")
        return varselUuid?.let {
            Aktivitetsplikt11_7Varsel(
                varselId = BrevbestillingReferanse(row.getUUID("brev_referanse")),
                sendtDato = row.getLocalDateOrNull("dato_varslet"),
                svarfrist = row.getLocalDateOrNull("frist")
            )
        }
    }
}