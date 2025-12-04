package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.overganguføre

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class OvergangUføreRepositoryImpl(private val connection: DBConnection) : OvergangUføreRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<OvergangUføreRepositoryImpl> {
        override fun konstruer(connection: DBConnection): OvergangUføreRepositoryImpl {
            return OvergangUføreRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): OvergangUføreGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT ID, VURDERINGER_ID
            FROM OVERGANG_UFORE_GRUNNLAG
            WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                OvergangUføreGrunnlag(
                    id = row.getLong("ID"),
                    vurderinger = mapOvergangUforevurderinger(row.getLongOrNull("VURDERINGER_ID"))
                )
            }
        }
    }

    private fun mapOvergangUforevurderinger(overgangUforevurderingerId: Long?): List<OvergangUføreVurdering> {
        return connection.queryList(
            """
                SELECT * FROM OVERGANG_UFORE_VURDERING WHERE VURDERINGER_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, overgangUforevurderingerId)
            }
            setRowMapper(::overgangUforevurderingRowMapper)
        }
    }

    private fun overgangUforevurderingRowMapper(row: Row): OvergangUføreVurdering {
        return OvergangUføreVurdering(
            begrunnelse = row.getString("BEGRUNNELSE"),
            brukerHarSøktOmUføretrygd = row.getBoolean("BRUKER_SOKT_UFORETRYGD"),
            brukerHarFåttVedtakOmUføretrygd = row.getStringOrNull("BRUKER_VEDTAK_UFORETRYGD"),
            brukerRettPåAAP = row.getBooleanOrNull("BRUKER_RETT_PAA_AAP"),
            vurdertIBehandling = row.getLongOrNull("VURDERT_I_BEHANDLING")?.let(::BehandlingId),
            fom = row.getLocalDateOrNull("VIRKNINGSDATO"), // Virkningsdato er 'vurderingen gjelder fra'
            vurdertAv = row.getString("VURDERT_AV"),
            opprettet = row.getInstant("OPPRETTET_TID"),
            tom = row.getLocalDateOrNull("TOM")
        )
    }

    override fun hentHistoriskeOvergangUforeVurderinger(
        sakId: SakId,
        behandlingId: BehandlingId
    ): List<OvergangUføreVurdering> {
        val query = """
            SELECT DISTINCT overgang_ufore_vurdering.*
            FROM overgang_ufore_grunnlag grunnlag
            INNER JOIN overgang_ufore_vurderinger ON grunnlag.vurderinger_id = overgang_ufore_vurderinger.id
            INNER JOIN overgang_ufore_vurdering ON overgang_ufore_vurdering.vurderinger_id = overgang_ufore_vurderinger.id
            INNER JOIN behandling ON grunnlag.behandling_id = behandling.id
            LEFT JOIN avbryt_revurdering_grunnlag ar ON ar.behandling_id = behandling.id
            WHERE grunnlag.aktiv AND behandling.sak_id = ?
                AND behandling.opprettet_tid < (select a.opprettet_tid from behandling a where id = ?)
                AND ar.behandling_id IS NULL
            ORDER BY overgang_ufore_vurdering.opprettet_tid
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.id)
                setLong(2, behandlingId.id)
            }
            setRowMapper(::overgangUforevurderingRowMapper)
        }
    }

    override fun lagre(behandlingId: BehandlingId, overgangUføreVurderinger: List<OvergangUføreVurdering>) {
        val overgangUforeGrunnlag = hentHvisEksisterer(behandlingId)

        val nyttGrunnlag = OvergangUføreGrunnlag(
            id = null,
            vurderinger = overgangUføreVurderinger
        )

        if (overgangUforeGrunnlag != nyttGrunnlag) {
            overgangUforeGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val overgangUforeVurderingerIds = getOvergangUforeVurderingerIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from overgang_ufore_grunnlag where behandling_id = ?; 
            delete from overgang_ufore_vurdering where vurderinger_id = ANY(?::bigint[]);
            delete from overgang_ufore_vurderinger where id = ANY(?::bigint[]);
           
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, overgangUforeVurderingerIds)
                setLongArray(3, overgangUforeVurderingerIds)
            }
        }
        log.info("Slettet $deletedRows rader fra overgang_ufore_grunnlag")
    }

    private fun getOvergangUforeVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT vurderinger_id
                    FROM overgang_ufore_grunnlag
                    WHERE behandling_id = ? AND vurderinger_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurderinger_id")
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: OvergangUføreGrunnlag) {
        val overgangUforevurderingerId = lagreOvergangUforevurderinger(nyttGrunnlag.vurderinger)

        connection.execute("INSERT INTO OVERGANG_UFORE_GRUNNLAG (BEHANDLING_ID, VURDERINGER_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, overgangUforevurderingerId)
            }
        }
    }

    private fun lagreOvergangUforevurderinger(vurderinger: List<OvergangUføreVurdering>): Long {
        val overganguforevurderingerId =
            connection.executeReturnKey("""INSERT INTO OVERGANG_UFORE_VURDERINGER DEFAULT VALUES""")

        connection.executeBatch(
            "INSERT INTO OVERGANG_UFORE_VURDERING (BEGRUNNELSE, BRUKER_SOKT_UFORETRYGD, BRUKER_VEDTAK_UFORETRYGD, BRUKER_RETT_PAA_AAP, VIRKNINGSDATO, VURDERT_AV, VURDERINGER_ID, VURDERT_I_BEHANDLING, TOM) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            vurderinger
        ) {
            setParams { vurdering ->
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.brukerHarSøktOmUføretrygd)
                setString(3, vurdering.brukerHarFåttVedtakOmUføretrygd)
                setBoolean(4, vurdering.brukerRettPåAAP)
                setLocalDate(5, vurdering.fom)
                setString(6, vurdering.vurdertAv)
                setLong(7, overganguforevurderingerId)
                setLong(8, vurdering.vurdertIBehandling?.toLong())
                setLocalDate(9, vurdering.tom)
            }
        }

        return overganguforevurderingerId
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE OVERGANG_UFORE_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute(
            """
            INSERT INTO OVERGANG_UFORE_GRUNNLAG (BEHANDLING_ID, VURDERINGER_ID) 
            SELECT ?, VURDERINGER_ID 
            FROM OVERGANG_UFORE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}