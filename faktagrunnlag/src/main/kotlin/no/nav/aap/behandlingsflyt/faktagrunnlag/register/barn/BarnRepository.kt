package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnRepository(private val connection: DBConnection) {
    fun hentHvisEksisterer(behandlingId: BehandlingId): BarnGrunnlag? {

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
                    oppgittBarn = hentOppgittBarn(it.getLongOrNull("oppgitt_barn_id")),
                    vurderteBarn = hentVurderteBarn(it.getLongOrNull("vurderte_barn_id"))
                )
            }
        }

        return grunnlag
    }

    fun hentHvisEksisterer(behandlingsreferanse: BehandlingReferanse): BarnGrunnlag? {
        val grunnlag = connection.queryFirstOrNull(
            """
            SELECT * 
            FROM BARNOPPLYSNING_GRUNNLAG g
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            WHERE g.AKTIV AND b.REFERANSE = ?
        """.trimIndent()
        ) {
            setParams {
                setString(1, behandlingsreferanse.toString())
            }
            setRowMapper {
                BarnGrunnlag(
                    registerbarn = hentBarn(it.getLongOrNull("register_barn_id")),
                    oppgittBarn = hentOppgittBarn(it.getLongOrNull("oppgitt_barn_id")),
                    vurderteBarn = hentVurderteBarn(it.getLongOrNull("vurderte_barn_id"))
                )
            }
        }

        return grunnlag
    }

    fun hentHvisEksisterer(saksnummer: Saksnummer): BarnGrunnlag? {
        val grunnlag = connection.queryFirstOrNull(
            """
            SELECT * 
            FROM BARNOPPLYSNING_GRUNNLAG g
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN SAK s ON b.SAK_ID = s.ID
            WHERE g.AKTIV AND s.SAKSNUMMER = ?
        """.trimIndent()
        ) {
            setParams {
                setString(1, saksnummer.toString())
            }
            setRowMapper {
                BarnGrunnlag(
                    registerbarn = hentBarn(it.getLongOrNull("register_barn_id")),
                    oppgittBarn = hentOppgittBarn(it.getLongOrNull("oppgitt_barn_id")),
                    vurderteBarn = hentVurderteBarn(it.getLongOrNull("vurderte_barn_id"))
                )
            }
        }

        return grunnlag
    }

    fun hent(behandlingId: BehandlingId): BarnGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    private fun hentOppgittBarn(id: Long?): OppgittBarn? {
        if (id == null) {
            return null
        }

        return OppgittBarn(
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
                    row.getPeriode("periode"),
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

        return RegisterBarn(id = id, identer = connection.queryList(
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

    fun hentOppgitteBarnForSaker(saksnumre: List<Saksnummer>): Map<Saksnummer, List<String>> {
        require(saksnumre.isNotEmpty())
        val placeholders = saksnumre.joinToString(",") { "?" }
        val res = connection.queryList(
            """
            SELECT DISTINCT s.SAKSNUMMER, ob.IDENT
            FROM BARNOPPLYSNING_GRUNNLAG g
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN SAK s ON b.SAK_ID = s.ID
            INNER JOIN OPPGITT_BARN ob ON ob.ID = g.OPPGITT_BARN_ID
            WHERE g.AKTIV AND s.SAKSNUMMER IN ($placeholders)
        """.trimIndent()
        ) {
            setParams {
                saksnumre.mapIndexed { index, saksnummer -> setString(index + 1, saksnummer.toString()) }
            }
            setRowMapper {
                Pair(Saksnummer(it.getString("saksnummer")), it.getString("ident"))
            }
        }
        return res
            .groupBy({ it.first }, { it.second })
    }

    fun hentRegisterBarnForSaker(saksnumre: List<Saksnummer>): Map<Saksnummer, List<String>> {
        require(saksnumre.isNotEmpty())
        val placeholders = saksnumre.joinToString(",") { "?" }
        val res = connection.queryList(
            """
            SELECT DISTINCT s.SAKSNUMMER, rb.IDENT
            FROM BARNOPPLYSNING_GRUNNLAG g
            INNER JOIN BEHANDLING b ON g.BEHANDLING_ID = b.ID
            INNER JOIN SAK s ON b.SAK_ID = s.ID
            INNER JOIN BARNOPPLYSNING rb ON rb.BGB_ID = g.REGISTER_BARN_ID
            WHERE g.AKTIV AND s.SAKSNUMMER IN ($placeholders)
        """.trimIndent()
        ) {
            setParams {
                saksnumre.mapIndexed { index, saksnummer -> setString(index + 1, saksnummer.toString()) }
            }
            setRowMapper {
                Pair(Saksnummer(it.getString("saksnummer")), it.getString("ident"))
            }
        }
        return res
            .groupBy({ it.first }, { it.second })
    }

    fun lagreOppgitteBarn(behandlingId: BehandlingId, oppgittBarn: OppgittBarn?) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val oppgittBarnId = if (oppgittBarn != null && oppgittBarn.identer.isNotEmpty()) {
            connection.executeReturnKey("INSERT INTO OPPGITT_BARNOPPLYSNING DEFAULT VALUES") {}
        } else {
            null
        }

        connection.executeBatch(
            """
                INSERT INTO OPPGITT_BARN (IDENT, oppgitt_barn_id) VALUES (?, ?)
            """.trimIndent(),
            requireNotNull(oppgittBarn?.identer)
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

    fun lagreRegisterBarn(behandlingId: BehandlingId, barn: Set<Ident>) {
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
                setLong(3, eksisterendeGrunnlag?.oppgittBarn?.id)
                setLong(4, eksisterendeGrunnlag?.vurderteBarn?.id)
            }
        }
    }

    fun lagreVurderinger(behandlingId: BehandlingId, vurderteBarn: List<VurdertBarn>) {
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
                INSERT INTO BARN_VURDERING_PERIODE (BARN_VURDERING_ID, PERIODE, BEGRUNNELSE, HAR_FORELDREANSVAR) VALUES (?, ?, ?, ?)
            """.trimIndent(), barn.vurderinger
            ) {
                setParams {
                    setLong(1, barnVurderingId)
                    setPeriode(2, it.periode)
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
                setLong(3, eksisterendeGrunnlag?.oppgittBarn?.id)
                setLong(4, vurderteBarnId)
            }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        val query = """
            INSERT INTO BARNOPPLYSNING_GRUNNLAG (behandling_id, register_barn_id, oppgitt_barn_id, vurderte_barn_id) SELECT ?, register_barn_id, oppgitt_barn_id, vurderte_barn_id from BARNOPPLYSNING_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, fraBehandling.toLong())
                setLong(2, tilBehandling.toLong())
            }
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
