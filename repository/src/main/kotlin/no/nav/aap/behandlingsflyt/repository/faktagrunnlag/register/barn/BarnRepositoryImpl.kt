package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.RegisterBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.VurderteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class BarnRepositoryImpl(private val connection: DBConnection) : BarnRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<BarnRepository> {
        override fun konstruer(connection: DBConnection): BarnRepository {
            return BarnRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): BarnGrunnlag? {
        val grunnlag = connection.queryFirstOrNull(
            """
            SELECT * 
            FROM BARNOPPLYSNING_GRUNNLAG g 
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper {
                BarnGrunnlag(
                    registerbarn = hentBarn(it.getLongOrNull("register_barn_id")),
                    oppgitteBarn = hentOppgittBarn(it.getLongOrNull("oppgitt_barn_id")),
                    vurderteBarn = hentVurderteBarn(it.getLongOrNull("vurderte_barn_id"))
                )
            }
        }

        return grunnlag
    }

    override fun hent(behandlingId: BehandlingId): BarnGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    private fun hentOppgittBarn(id: Long?): OppgitteBarn? {
        if (id == null) {
            return null
        }

        return OppgitteBarn(
            id, connection.queryList(
                """
                SELECT p.IDENT
                FROM OPPGITT_BARN p
                WHERE p.oppgitt_barn_id = ?
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, id)
                }
                setRowMapper { row ->
                    Ident(
                        row.getString("IDENT")
                    )
                }
            }.toSet()
        )
    }

    private fun hentVurderteBarn(id: Long?): VurderteBarn? {
        if (id == null) {
            return null
        }

        return VurderteBarn(
            id, connection.queryList(
                """
                SELECT p.id, p.IDENT
                FROM BARN_VURDERING p
                WHERE p.BARN_VURDERINGER_ID = ?
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, id)
                }
                setRowMapper { row ->
                    VurdertBarn(
                        ident = Ident(row.getString("IDENT")), vurderinger = hentVurderinger(row.getLong("id"))
                    )
                }
            }
        )
    }

    private fun hentVurderinger(vurdertBarnId: Long): List<VurderingAvForeldreAnsvar> {
        return connection.queryList(
            """
                SELECT periode, HAR_FORELDREANSVAR, BEGRUNNELSE
                FROM BARN_VURDERING_PERIODE
                WHERE BARN_VURDERING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, vurdertBarnId)
            }
            setRowMapper { row ->
                VurderingAvForeldreAnsvar(
                    row.getPeriode("periode").fom,
                    row.getBoolean("HAR_FORELDREANSVAR"),
                    row.getString("BEGRUNNELSE")
                )
            }
        }
    }

    private fun hentBarn(id: Long?): RegisterBarn? {
        if (id == null) {
            return null
        }

        return RegisterBarn(
            id = id, identer = connection.queryList(
                """
                SELECT p.IDENT
                FROM BARNOPPLYSNING p
                WHERE p.bgb_id = ?
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, id)
                }
                setRowMapper { row ->
                    Ident(
                        row.getString("IDENT")
                    )
                }
            })
    }

    override fun lagreOppgitteBarn(behandlingId: BehandlingId, oppgitteBarn: OppgitteBarn?) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val alleOppgitteBarn = HashSet(eksisterendeGrunnlag?.oppgitteBarn?.identer ?: emptySet())
        alleOppgitteBarn.addAll(oppgitteBarn?.identer ?: emptySet())

        val oppgittBarnId = if (alleOppgitteBarn.isNotEmpty()) {
            connection.executeReturnKey("INSERT INTO OPPGITT_BARNOPPLYSNING DEFAULT VALUES") {}
        } else {
            null
        }

        connection.executeBatch(
            """
                INSERT INTO OPPGITT_BARN (IDENT, oppgitt_barn_id) VALUES (?, ?)
            """.trimIndent(),
            alleOppgitteBarn
        ) {
            setParams { barnet ->
                setString(1, barnet.identifikator)
                setLong(2, oppgittBarnId)
            }
        }

        connection.execute(
            """
                INSERT INTO BARNOPPLYSNING_GRUNNLAG (BEHANDLING_ID, register_barn_id, oppgitt_barn_id, vurderte_barn_id) VALUES (?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, eksisterendeGrunnlag?.registerbarn?.id)
                setLong(3, oppgittBarnId)
                setLong(4, eksisterendeGrunnlag?.vurderteBarn?.id)
            }
        }
    }

    override fun lagreRegisterBarn(behandlingId: BehandlingId, barn: Set<Ident>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val bgbId = connection.executeReturnKey("INSERT INTO BARNOPPLYSNING_GRUNNLAG_BARNOPPLYSNING DEFAULT VALUES") {}

        connection.executeBatch(
            """
                INSERT INTO BARNOPPLYSNING (IDENT, BGB_ID) VALUES (?, ?)
            """.trimIndent(),
            barn
        ) {
            setParams { barnet ->
                setString(1, barnet.identifikator)
                setLong(2, bgbId)
            }
        }

        connection.execute(
            """
                INSERT INTO BARNOPPLYSNING_GRUNNLAG (BEHANDLING_ID, register_barn_id, oppgitt_barn_id, vurderte_barn_id) VALUES (?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, bgbId)
                setLong(3, eksisterendeGrunnlag?.oppgitteBarn?.id)
                setLong(4, eksisterendeGrunnlag?.vurderteBarn?.id)
            }
        }
    }

    override fun lagreVurderinger(behandlingId: BehandlingId, vurderteBarn: List<VurdertBarn>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val vurderteBarnId = if (vurderteBarn.isNotEmpty()) {
            connection.executeReturnKey("INSERT INTO BARN_VURDERINGER DEFAULT VALUES") {}
        } else {
            null
        }

        for (barn in vurderteBarn) {
            val barnVurderingId =
                connection.executeReturnKey("INSERT INTO BARN_VURDERING (IDENT, BARN_VURDERINGER_ID) VALUES (?, ?)") {
                    setParams {
                        setString(1, barn.ident.identifikator)
                        setLong(2, vurderteBarnId)
                    }
                }
            connection.executeBatch(
                """
                INSERT INTO BARN_VURDERING_PERIODE (BARN_VURDERING_ID, PERIODE, BEGRUNNELSE, HAR_FORELDREANSVAR) VALUES (?, ?::daterange, ?, ?)
            """.trimIndent(), barn.vurderinger
            ) {
                setParams {
                    setLong(1, barnVurderingId)
                    setPeriode(2, Periode(it.fraDato, it.fraDato))
                    setString(3, it.begrunnelse)
                    setBoolean(4, it.harForeldreAnsvar)
                }
            }
        }

        connection.execute(
            """
                INSERT INTO BARNOPPLYSNING_GRUNNLAG (BEHANDLING_ID, register_barn_id, oppgitt_barn_id, vurderte_barn_id) VALUES (?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, eksisterendeGrunnlag?.registerbarn?.id)
                setLong(3, eksisterendeGrunnlag?.oppgitteBarn?.id)
                setLong(4, vurderteBarnId)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        val query = """
            INSERT INTO BARNOPPLYSNING_GRUNNLAG
                (behandling_id, register_barn_id, oppgitt_barn_id, vurderte_barn_id)
            SELECT ?, register_barn_id, oppgitt_barn_id, vurderte_barn_id
                from BARNOPPLYSNING_GRUNNLAG
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

        val oppgittBarnIds = getOppgittBarnIds(behandlingId)
        val registerBarnIds = getRegisterBarnIds(behandlingId)
        val vurderteBarnIds = getVurderteBarnIds(behandlingId)
        val barnVurderingIds = getBarnVurderingIds(vurderteBarnIds)
        val barnOpplysningIds = getBarnOpplysningIds(registerBarnIds)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from barnopplysning_grunnlag where behandling_id = ?;
            delete from barn_vurdering where id = ANY(?::bigint[]);
            delete from barn_vurdering_periode where id = ANY(?::bigint[]);
            delete from barn_vurderinger where id = ANY(?::bigint[]);
            delete from barnopplysning where id = ANY(?::bigint[]);
            delete from barnopplysning_grunnlag_barnopplysning where id = ANY(?::bigint[]);
            delete from oppgitt_barn where oppgitt_barn_id = ANY(?::bigint[]);   
            delete from oppgitt_barnopplysning where id = ANY(?::bigint[]);          
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, barnVurderingIds)
                setLongArray(3, barnVurderingIds)
                setLongArray(4, vurderteBarnIds)
                setLongArray(5, barnOpplysningIds)
                setLongArray(6, registerBarnIds)
                setLongArray(7, oppgittBarnIds)
                setLongArray(8, oppgittBarnIds)

            }
        }
        log.info("Slettet $deletedRows fra barnopplysning_grunnlag")
    }

    private fun getOppgittBarnIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT oppgitt_barn_id
                    FROM barnopplysning_grunnlag
                    WHERE behandling_id = ? AND oppgitt_barn_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("oppgitt_barn_id")
        }
    }

    private fun getRegisterBarnIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT register_barn_id
                    FROM barnopplysning_grunnlag
                    WHERE behandling_id = ? AND register_barn_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("register_barn_id")
        }
    }

    private fun getVurderteBarnIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT vurderte_barn_id
                    FROM barnopplysning_grunnlag
                    WHERE behandling_id = ? AND vurderte_barn_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurderte_barn_id")
        }
    }

    private fun getBarnVurderingIds(vurderingerIds: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM barn_vurdering
                    WHERE barn_vurderinger_id = ANY(?::bigint[]);
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, vurderingerIds) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }

    private fun getBarnOpplysningIds(registerBarnIds: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM barnopplysning
                    WHERE bgb_id = ANY(?::bigint[]);
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, registerBarnIds) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }


    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE BARNOPPLYSNING_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }
}
