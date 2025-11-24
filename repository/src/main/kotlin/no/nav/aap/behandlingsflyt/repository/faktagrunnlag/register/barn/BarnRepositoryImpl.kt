package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.RegisterBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Relasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.SaksbehandlerOppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.VurderteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
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
                    registerbarn = it.getLongOrNull("register_barn_id")?.let(::hentBarn),
                    oppgitteBarn = it.getLongOrNull("oppgitt_barn_id")?.let(::hentOppgittBarn),
                    saksbehandlerOppgitteBarn = it.getLongOrNull("saksbehandler_oppgitt_barn_id")
                        ?.let(::hentSaksbehandlerOppgitteBarn),
                    vurderteBarn = it.getLongOrNull("vurderte_barn_id")?.let(::hentVurderteBarn)
                )
            }
        }

        return grunnlag
    }

    override fun hentVurderteBarnHvisEksisterer(behandlingId: BehandlingId): VurderteBarn? {
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
                it.getLongOrNull("vurderte_barn_id")?.let(::hentVurderteBarn)
            }
        }

        return grunnlag
    }

    override fun hent(behandlingId: BehandlingId): BarnGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    override fun hentBehandlingIdForSakSomFårBarnetilleggForBarn(ident: Ident): List<BehandlingId> {
        log.info("Henter info for ident {}", ident)
        val registerBarnId = getRegisterBarnId(ident)
        log.info("Henter registerbarnid for registerBarnId {}", registerBarnId)
        if (registerBarnId != null) {
            val behandlingId = hentBehandlingIdForBarneId(registerBarnId)
            log.info("Henter behandling for behandlingId {}", behandlingId)
            return behandlingId
        }
        return emptyList()
    }


    private fun hentBehandlingIdForBarneId(id: Long): List<BehandlingId> {


        val behandlingIds = connection.queryList(
            """
            SELECT BEHANDLING_ID 
            FROM BARNOPPLYSNING_GRUNNLAG g 
            WHERE g.AKTIV AND g.REGISTER_BARN_ID = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, id)
            }
            setRowMapper {
                BehandlingId(
                    id = it.getLong("behandling_id"),
                )
            }
        }

        return behandlingIds
    }


    private fun getRegisterBarnId(ident: Ident): Long? = connection.queryFirstOrNull(
        """
                    SELECT bgb_id
                    FROM barnopplysning
                    WHERE ident = ? AND ident is not null
                 
                """.trimIndent()
    ) {
        setParams { setString(1, ident.identifikator) }
        setRowMapper { row ->
            row.getLong("bgb_id")
        }
    }

    private fun hentSaksbehandlerOppgitteBarn(id: Long): SaksbehandlerOppgitteBarn {
        return SaksbehandlerOppgitteBarn(
            id, connection.queryList(
                """
                SELECT p.ident, p.navn, p.fodselsdato, p.relasjon
                FROM BARN_SAKSBEHANDLER_OPPGITT p
                WHERE p.saksbehandler_oppgitt_barn_id = ?
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, id)
                }
                setRowMapper { row ->
                    SaksbehandlerOppgitteBarn.SaksbehandlerOppgitteBarn(
                        ident = row.getStringOrNull("ident")?.let(::Ident),
                        navn = row.getString("navn"),
                        fødselsdato = Fødselsdato(row.getLocalDate("fodselsdato")),
                        relasjon = row.getString("relasjon").let(Relasjon::valueOf),
                    )
                }
            }
        )
    }

    private fun hentOppgittBarn(id: Long): OppgitteBarn {
        return OppgitteBarn(
            id, connection.queryList(
                """
                SELECT p.IDENT, p.navn, p.fodselsdato, p.relasjon
                FROM OPPGITT_BARN p
                WHERE p.oppgitt_barn_id = ?
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, id)
                }
                setRowMapper { row ->
                    OppgitteBarn.OppgittBarn(
                        ident = row.getStringOrNull("IDENT")?.let(::Ident),
                        navn = row.getStringOrNull("navn"),
                        fødselsdato = row.getLocalDateOrNull("fodselsdato")?.let(::Fødselsdato),
                        relasjon = row.getStringOrNull("relasjon")?.let(Relasjon::valueOf),
                    )
                }
            }
        )
    }

    private fun hentVurderteBarn(id: Long): VurderteBarn {
        return connection.queryFirst(
            """
            SELECT * FROM BARN_VURDERINGER WHERE ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, id)
            }
            setRowMapper { row ->
                VurderteBarn(
                    id = id,
                    barn = hentBarnVurderinger(id),
                    vurdertAv = row.getString("VURDERT_AV"),
                    vurdertTidspunkt = row.getLocalDateTime("OPPRETTET_TID")
                )
            }
        }
    }

    private fun hentBarnVurderinger(id: Long?) =
        connection.queryList(
            """
            SELECT p.id, p.IDENT, p.navn, p.fodselsdato
            FROM BARN_VURDERING p
            WHERE p.BARN_VURDERINGER_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, id)
            }
            setRowMapper { row ->
                val identifikator = row.getStringOrNull("IDENT")
                val barnIdentifikator = if (identifikator != null) {
                    BarnIdentifikator.BarnIdent(Ident(identifikator))
                } else {
                    BarnIdentifikator.NavnOgFødselsdato(
                        row.getString("navn"),
                        row.getLocalDate("fodselsdato").let(::Fødselsdato)
                    )
                }
                VurdertBarn(
                    ident = barnIdentifikator,
                    vurderinger = hentVurderinger(row.getLong("id"))
                )
            }
        }

    private fun hentVurderinger(vurdertBarnId: Long): List<VurderingAvForeldreAnsvar> {
        return connection.queryList(
            """
                SELECT periode, HAR_FORELDREANSVAR, BEGRUNNELSE, ER_FOSTERFORELDER
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
                    row.getString("BEGRUNNELSE"),
                    row.getBooleanOrNull("ER_FOSTERFORELDER"),
                )
            }
        }
    }

    private fun hentBarn(id: Long): RegisterBarn {
        return RegisterBarn(
            id = id, barn = connection.queryList(
                """
                SELECT p.IDENT, fodselsdato, dodsdato, navn
                FROM BARNOPPLYSNING p
                WHERE p.bgb_id = ?
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, id)
                }
                setRowMapper { row ->
                    val fødselsdato = Fødselsdato(row.getLocalDate("fodselsdato"))

                    val identifikator = if (row.getStringOrNull("IDENT") != null) {
                        BarnIdentifikator.BarnIdent(Ident(row.getString("IDENT")))
                    } else if (row.getStringOrNull("navn") != null) {
                        BarnIdentifikator.NavnOgFødselsdato(row.getString("navn"), fødselsdato)
                    } else {
                        throw IllegalStateException("Krever enten ident eller navn+fødselsdato for registerbarn.")
                    }

                    Barn(
                        ident = identifikator,
                        fødselsdato = fødselsdato,
                        dødsdato = row.getLocalDateOrNull("dodsdato")?.let(::Dødsdato),
                        navn = row.getStringOrNull("navn")
                    )
                }
            })
    }


    override fun lagreOppgitteBarn(behandlingId: BehandlingId, oppgitteBarn: OppgitteBarn) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val oppgittBarn = oppgitteBarn.oppgitteBarn

        val oppgittBarnId = if (oppgittBarn.isNotEmpty()) {
            connection.executeReturnKey("INSERT INTO OPPGITT_BARNOPPLYSNING DEFAULT VALUES")
        } else {
            null
        }

        connection.executeBatch(
            """
            INSERT INTO OPPGITT_BARN (IDENT, oppgitt_barn_id, navn, fodselsdato, relasjon)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            oppgittBarn
        ) {
            setParams { barnet ->
                setString(1, barnet.ident?.identifikator)
                setLong(2, oppgittBarnId)
                setString(3, barnet.navn)
                setLocalDate(4, barnet.fødselsdato?.toLocalDate())
                setString(5, barnet.relasjon?.name)
            }
        }

        connection.execute(
            """
                INSERT INTO BARNOPPLYSNING_GRUNNLAG (BEHANDLING_ID, register_barn_id, oppgitt_barn_id, vurderte_barn_id, saksbehandler_oppgitt_barn_id) VALUES (?,?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, eksisterendeGrunnlag?.registerbarn?.id)
                setLong(3, oppgittBarnId)
                setLong(4, eksisterendeGrunnlag?.vurderteBarn?.id)
                setLong(5, eksisterendeGrunnlag?.saksbehandlerOppgitteBarn?.id)
            }
        }
    }

    override fun lagreSaksbehandlerOppgitteBarn(
        behandlingId: BehandlingId,
        saksbehandlerOppgitteBarn: List<SaksbehandlerOppgitteBarn.SaksbehandlerOppgitteBarn>
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val saksbehandlerOppgitteBarnId = if (saksbehandlerOppgitteBarn.isNotEmpty()) {
            connection.executeReturnKey("INSERT INTO BARN_SAKSBEHANDLER_OPPGITT_BARNOPPLYSNING DEFAULT VALUES")
        } else {
            null
        }

        connection.executeBatch(
            """
        INSERT INTO BARN_SAKSBEHANDLER_OPPGITT (IDENT, saksbehandler_oppgitt_barn_id, navn, fodselsdato, relasjon)
        VALUES (?, ?, ?, ?, ?)
        """.trimIndent(),
            saksbehandlerOppgitteBarn
        ) {
            setParams { barnet ->
                setString(1, barnet.ident?.identifikator)
                setLong(2, saksbehandlerOppgitteBarnId)
                setString(3, barnet.navn)
                setLocalDate(4, barnet.fødselsdato.toLocalDate())
                setString(5, barnet.relasjon.name)
            }
        }

        connection.execute(
            """
            INSERT INTO BARNOPPLYSNING_GRUNNLAG (BEHANDLING_ID, register_barn_id, oppgitt_barn_id, vurderte_barn_id, saksbehandler_oppgitt_barn_id) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, eksisterendeGrunnlag?.registerbarn?.id)
                setLong(3, eksisterendeGrunnlag?.oppgitteBarn?.id)
                setLong(4, eksisterendeGrunnlag?.vurderteBarn?.id)
                setLong(5, saksbehandlerOppgitteBarnId)
            }
        }
    }

    override fun lagreRegisterBarn(behandlingId: BehandlingId, barn: Map<Barn, PersonId?>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val bgbId = connection.executeReturnKey("INSERT INTO BARNOPPLYSNING_GRUNNLAG_BARNOPPLYSNING DEFAULT VALUES")

        connection.executeBatch(
            """
                INSERT INTO BARNOPPLYSNING (IDENT, BGB_ID, fodselsdato, dodsdato, person_id, navn)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            barn.entries
        ) {
            setParams { barnet ->
                val (barn, personId) = barnet
                setString(
                    1,
                    (barn.ident as? BarnIdentifikator.BarnIdent)?.ident?.identifikator
                )
                setLong(2, bgbId)
                setLocalDate(3, barn.fødselsdato.toLocalDate())
                setLocalDate(4, barn.dødsdato?.toLocalDate())
                setLong(5, personId?.id)
                setString(6, barn.navn)
            }
        }

        connection.execute(
            """
                INSERT INTO BARNOPPLYSNING_GRUNNLAG (BEHANDLING_ID, register_barn_id, oppgitt_barn_id, vurderte_barn_id, saksbehandler_oppgitt_barn_id) VALUES (?,?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, bgbId)
                setLong(3, eksisterendeGrunnlag?.oppgitteBarn?.id)
                setLong(4, eksisterendeGrunnlag?.vurderteBarn?.id)
                setLong(5, eksisterendeGrunnlag?.saksbehandlerOppgitteBarn?.id)
            }
        }
    }

    override fun lagreVurderinger(behandlingId: BehandlingId, vurdertAv: String, vurderteBarn: List<VurdertBarn>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val vurderteBarnId = if (vurderteBarn.isNotEmpty()) {
            connection.executeReturnKey(
                """
                INSERT INTO BARN_VURDERINGER (VURDERT_AV)
                VALUES (?)
                """.trimIndent()
            ) {
                setParams {
                    setString(1, vurdertAv)
                }
            }
        } else {
            null
        }

        lagreVurderingerMedPerioder(vurderteBarnId, vurderteBarn)

        connection.execute(
            """
            INSERT INTO BARNOPPLYSNING_GRUNNLAG (BEHANDLING_ID, register_barn_id, oppgitt_barn_id, saksbehandler_oppgitt_barn_id, vurderte_barn_id) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, eksisterendeGrunnlag?.registerbarn?.id)
                setLong(3, eksisterendeGrunnlag?.oppgitteBarn?.id)
                setLong(4, eksisterendeGrunnlag?.saksbehandlerOppgitteBarn?.id)
                setLong(5, vurderteBarnId)
            }
        }
    }

    private fun lagreVurderingerMedPerioder(vurderteBarnId: Long?, vurderteBarn: List<VurdertBarn>) {
        for (barn in vurderteBarn) {
            val barnVurderingId =
                connection.executeReturnKey(
                    """INSERT INTO BARN_VURDERING (IDENT, BARN_VURDERINGER_ID, navn, fodselsdato) VALUES (?, ?, ?, ?)"""
                ) {
                    setParams {
                        when (val barnIdent = barn.ident) {
                            is BarnIdentifikator.BarnIdent -> {
                                setString(1, barnIdent.ident.identifikator)
                                setLong(2, vurderteBarnId)
                                setString(3, null)
                                setLocalDate(4, null)
                            }

                            is BarnIdentifikator.NavnOgFødselsdato -> {
                                setString(1, null)
                                setLong(2, vurderteBarnId)
                                setString(3, barnIdent.navn)
                                setLocalDate(4, barnIdent.fødselsdato.toLocalDate())
                            }
                        }
                    }
                }
            connection.executeBatch(
                """
                INSERT INTO BARN_VURDERING_PERIODE (BARN_VURDERING_ID, PERIODE, BEGRUNNELSE, HAR_FORELDREANSVAR, ER_FOSTERFORELDER) VALUES (?, ?::daterange, ?, ?, ?)
            """.trimIndent(), barn.vurderinger
            ) {
                setParams {
                    setLong(1, barnVurderingId)
                    setPeriode(2, Periode(it.fraDato, it.fraDato))
                    setString(3, it.begrunnelse)
                    setBoolean(4, it.harForeldreAnsvar)
                    setBoolean(5, it.erFosterForelder)
                }
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        val query = """
            INSERT INTO BARNOPPLYSNING_GRUNNLAG
                (behandling_id, register_barn_id, oppgitt_barn_id, vurderte_barn_id, saksbehandler_oppgitt_barn_id)
            SELECT ?, register_barn_id, oppgitt_barn_id, vurderte_barn_id, saksbehandler_oppgitt_barn_id
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

    fun kopierMedNyeBarn(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId,
        nyRegisterBarnId: Long?,
        nyOppgittBarnId: Long?,
    ) {
        require(fraBehandling != tilBehandling)
        val fraGrunnlag = hentHvisEksisterer(fraBehandling)

        if (fraGrunnlag != null) {
            connection.execute(
                """
            INSERT INTO BARNOPPLYSNING_GRUNNLAG
                (behandling_id, register_barn_id, oppgitt_barn_id, vurderte_barn_id, saksbehandler_oppgitt_barn_id)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, tilBehandling.toLong())
                    setLong(2, nyRegisterBarnId ?: fraGrunnlag.registerbarn?.id)
                    setLong(3, nyOppgittBarnId ?: fraGrunnlag.oppgitteBarn?.id)
                    setLong(4, fraGrunnlag.vurderteBarn?.id)
                    setLong(5, fraGrunnlag.saksbehandlerOppgitteBarn?.id)
                }
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
            delete from barn_vurdering_periode where barn_vurdering_id = ANY(?::bigint[]);
            delete from barn_vurdering where id = ANY(?::bigint[]);
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
        log.info("Slettet $deletedRows rader fra barnopplysning_grunnlag")
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

    override fun tilbakestillGrunnlag(behandlingId: BehandlingId, forrigeBehandlingId: BehandlingId?) {
        val barnGrunnlag = hentHvisEksisterer(behandlingId)
        val forrigeBarnGrunnlag = forrigeBehandlingId?.let { hentHvisEksisterer(it) }

        if (forrigeBehandlingId == null) {
            lagreVurderinger(behandlingId, SYSTEMBRUKER.ident, emptyList())
            deaktiverAlleSaksbehandlerOppgitteBarn(behandlingId)
            return
        }

        if (barnGrunnlag == null || forrigeBarnGrunnlag == null) {
            return
        }

        val harNyeBarn = harNyeBarnISammenligningMedForrige(barnGrunnlag, forrigeBarnGrunnlag)

        if (!harNyeBarn) {
            deaktiverEksisterende(behandlingId)
            kopier(forrigeBehandlingId, behandlingId)
            return
        }

        tilbakestillMedNyeBarn(behandlingId, forrigeBehandlingId, barnGrunnlag, forrigeBarnGrunnlag)
    }

    private fun harNyeBarnISammenligningMedForrige(barnGrunnlag: BarnGrunnlag, forrigeBarnGrunnlag: BarnGrunnlag): Boolean {
        val flereRegisterBarn = (barnGrunnlag.registerbarn?.barn?.size ?: 0) > (forrigeBarnGrunnlag.registerbarn?.barn?.size ?: 0)
        val flereOppgitteBarn = (barnGrunnlag.oppgitteBarn?.oppgitteBarn?.size ?: 0) > (forrigeBarnGrunnlag.oppgitteBarn?.oppgitteBarn?.size ?: 0)
        val flereVurderteBarn = (barnGrunnlag.vurderteBarn?.barn?.size ?: 0) > (forrigeBarnGrunnlag.vurderteBarn?.barn?.size ?: 0)

        return flereRegisterBarn || flereOppgitteBarn || flereVurderteBarn
    }

    private fun tilbakestillMedNyeBarn(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId,
        barnGrunnlag: BarnGrunnlag,
        forrigeBarnGrunnlag: BarnGrunnlag
    ) {
        deaktiverEksisterende(behandlingId)

        val nyRegisterBarnId = if ((barnGrunnlag.registerbarn?.barn?.size ?: 0) > (forrigeBarnGrunnlag.registerbarn?.barn?.size ?: 0)) {
            barnGrunnlag.registerbarn?.id
        } else null

        val nyOppgittBarnId = if ((barnGrunnlag.oppgitteBarn?.oppgitteBarn?.size ?: 0) > (forrigeBarnGrunnlag.oppgitteBarn?.oppgitteBarn?.size ?: 0)) {
            barnGrunnlag.oppgitteBarn?.id
        } else null

        kopierMedNyeBarn(forrigeBehandlingId, behandlingId, nyRegisterBarnId, nyOppgittBarnId)

        // // Tilbakestill vurderte barn til forrige tilstand. Nye barn uten tidligere vurderinger (fra forrigeBarnGrunnlag)
        // blir ikke lagret og forblir dermed uvurderte (nullstilt).
        tilbakestillVurderteBarnetVedBehov(behandlingId, barnGrunnlag, forrigeBarnGrunnlag)
    }

    private fun tilbakestillVurderteBarnetVedBehov(behandlingId: BehandlingId, barnGrunnlag: BarnGrunnlag, forrigeBarnGrunnlag: BarnGrunnlag) {
        val vurderteBarnNå = barnGrunnlag.vurderteBarn?.barn.orEmpty()
        val vurderteBarnForrige = forrigeBarnGrunnlag.vurderteBarn?.barn.orEmpty()

        if (vurderteBarnNå.size <= vurderteBarnForrige.size) return

        lagreVurderinger(
            behandlingId = behandlingId,
            vurdertAv = barnGrunnlag.vurderteBarn?.vurdertAv ?: "system",
            vurderteBarn = vurderteBarnForrige
        )
    }

    /**
     * Deaktiver alle saksbehandleroppgitte barn for en behandling. Brukes når alle saksbehandleroppgitte barn skal fjernes.
     */
    private fun deaktiverAlleSaksbehandlerOppgitteBarn(behandlingId: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeGrunnlag?.saksbehandlerOppgitteBarn != null) {
            deaktiverEksisterende(behandlingId)

            connection.execute(
                """
                INSERT INTO BARNOPPLYSNING_GRUNNLAG (BEHANDLING_ID, register_barn_id, oppgitt_barn_id, vurderte_barn_id, saksbehandler_oppgitt_barn_id)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
            ) {
                setParams {
                    setLong(1, behandlingId.toLong())
                    setLong(2, eksisterendeGrunnlag.registerbarn?.id)
                    setLong(3, eksisterendeGrunnlag.oppgitteBarn?.id)
                    setLong(4, eksisterendeGrunnlag.vurderteBarn?.id)
                    setLong(5, null)
                }
            }
        }
    }
}
