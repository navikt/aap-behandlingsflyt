package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Gjenopptak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Klage
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Tilleggsopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.TrukketSøknad
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory

class KravRepositoryImpl(private val connection: DBConnection) : KravRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<KravRepositoryImpl> {
        override fun konstruer(connection: DBConnection): KravRepositoryImpl {
            return KravRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, vurderinger: List<KravVurdering>) {
        val eksisterende = hentHvisEksisterer(behandlingId)
        val nytt = KravGrunnlag(vurderinger)

        if (eksisterende != nytt) {
            eksisterende?.let { deaktiverGrunnlag(behandlingId) }
            lagreGrunnlag(behandlingId, nytt)
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute(
            "UPDATE krav_grunnlag SET aktiv = FALSE WHERE behandling_id = ? AND aktiv = TRUE"
        ) {
            setParams { setLong(1, behandlingId.id) }
            setResultValidator { require(it == 1) }
        }
    }

    private fun lagreGrunnlag(behandlingId: BehandlingId, grunnlag: KravGrunnlag) {
        val vurderingerId = connection.executeReturnKey("INSERT INTO krav_vurderinger DEFAULT VALUES")

        connection.executeBatch(
            """
            INSERT INTO krav_vurdering (
                krav_vurderinger_id, journalpost_id, vurdert_av, opprettet_tid,
                krav_type, soknadsdato, soknadsdato_aarsak,
                mulig_rett_fra, mulig_rett_fra_aarsak,
                begrunnelse, kravdato, vurdert_i_behandling
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            grunnlag.vurderinger
        ) {
            setParams { v ->
                setLong(1, vurderingerId)
                setString(2, v.journalpostId.identifikator)
                setString(3, v.vurdertAv)
                setInstant(4, v.opprettet)
                setString(10, v.begrunnelse)
                setLong(12, v.vurdertIBehandling.id)
                when (v) {
                    is NyttKrav -> {
                        setEnumName(5, KravType.NYTT_KRAV_AAP)
                        setLocalDate(6, v.soknadsdato)
                        setEnumName(7, v.soknadsdatoÅrsak)
                        setLocalDate(8, v.muligRettFra)
                        setEnumName(9, v.muligRettFraÅrsak)
                        setLocalDate(11, v.kravdato)
                    }

                    is Gjenopptak -> {
                        setEnumName(5, KravType.GJENOPPTAK)
                        setLocalDate(6, v.soknadsdato)
                        setEnumName(7, v.soknadsdatoÅrsak)
                        setLocalDate(8, v.muligRettFra)
                        setEnumName(9, v.muligRettFraÅrsak)
                        setLocalDate(11, v.kravdato)
                    }

                    is TrukketSøknad -> {
                        setEnumName(5, KravType.TRUKKET_SOKNAD)
                        setLocalDate(6, null)
                        setEnumName(7, null as Enum<*>?)
                        setLocalDate(8, null)
                        setEnumName(9, null as Enum<*>?)
                        setLocalDate(11, null)
                    }

                    is Klage -> {
                        setEnumName(5, KravType.KLAGE)
                        setLocalDate(6, null)
                        setEnumName(7, null as Enum<*>?)
                        setLocalDate(8, null)
                        setEnumName(9, null as Enum<*>?)
                        setLocalDate(11, null)
                    }

                    is Tilleggsopplysning -> {
                        setEnumName(5, KravType.TILLEGGSOPPLYSNING)
                        setLocalDate(6, null)
                        setEnumName(7, null as Enum<*>?)
                        setLocalDate(8, null)
                        setEnumName(9, null as Enum<*>?)
                        setLocalDate(11, null)
                    }
                }
            }
        }

        connection.execute(
            "INSERT INTO krav_grunnlag (behandling_id, krav_vurderinger_id) VALUES (?, ?)"
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingerId)
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): KravGrunnlag? {
        return connection.queryFirstOrNull(
            "SELECT krav_vurderinger_id FROM krav_grunnlag WHERE behandling_id = ? AND aktiv = TRUE"
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row ->
                KravGrunnlag(hentVurderinger(row.getLong("krav_vurderinger_id")))
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): KravGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId)) {
            "Fant ikke kravgrunnlag for behandling med ID $behandlingId."
        }
    }

    private fun hentVurderinger(vurderingerId: Long): List<KravVurdering> {
        return connection.queryList(
            """
            SELECT journalpost_id, vurdert_av, krav_type,
                   soknadsdato, soknadsdato_aarsak,
                   mulig_rett_fra, mulig_rett_fra_aarsak,
                   begrunnelse, kravdato, vurdert_i_behandling, opprettet_tid
            FROM krav_vurdering
            WHERE krav_vurderinger_id = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, vurderingerId) }
            setRowMapper(::mapVurdering)
        }
    }

    private fun mapVurdering(row: Row): KravVurdering {
        val journalpostId = JournalpostId(row.getString("journalpost_id"))
        val vurdertAv = row.getString("vurdert_av")
        val opprettet = row.getInstant("opprettet_tid")
        val begrunnelse = row.getString("begrunnelse")
        val vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling"))

        return when (val kravType = row.getEnum<KravType>("krav_type")) {
            KravType.NYTT_KRAV_AAP -> NyttKrav(
                journalpostId = journalpostId, vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
                soknadsdato = row.getLocalDate("soknadsdato"),
                soknadsdatoÅrsak = row.getEnumOrNull("soknadsdato_aarsak"),
                muligRettFra = row.getLocalDateOrNull("mulig_rett_fra"),
                muligRettFraÅrsak = row.getEnumOrNull("mulig_rett_fra_aarsak"),
                kravdato = row.getLocalDate("kravdato"),
            )

            KravType.GJENOPPTAK -> Gjenopptak(
                journalpostId = journalpostId, vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
                soknadsdato = row.getLocalDateOrNull("soknadsdato"),
                soknadsdatoÅrsak = row.getEnumOrNull("soknadsdato_aarsak"),
                muligRettFra = row.getLocalDateOrNull("mulig_rett_fra"),
                muligRettFraÅrsak = row.getEnumOrNull("mulig_rett_fra_aarsak"),
                kravdato = row.getLocalDate("kravdato"),
            )

            KravType.TRUKKET_SOKNAD -> TrukketSøknad(
                journalpostId = journalpostId, vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
            )

            KravType.KLAGE -> Klage(
                journalpostId = journalpostId, vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
            )

            KravType.TILLEGGSOPPLYSNING -> Tilleggsopplysning(
                journalpostId = journalpostId, vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
            )
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val vurderingerIder = hentVurderingerIder(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            DELETE FROM krav_grunnlag WHERE behandling_id = ?;
            DELETE FROM krav_vurdering WHERE krav_vurderinger_id = ANY(?::bigint[]);
            DELETE FROM krav_vurderinger WHERE id = ANY(?::bigint[]);
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, vurderingerIder)
                setLongArray(3, vurderingerIder)
            }
        }
        log.info("Slettet $deletedRows rader fra krav-tabeller for behandling $behandlingId")
    }

    private fun hentVurderingerIder(behandlingId: BehandlingId): List<Long> {
        return connection.queryList(
            """
            SELECT krav_vurderinger_id
            FROM krav_grunnlag
            WHERE behandling_id = ? AND krav_vurderinger_id IS NOT NULL
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row -> row.getLong("krav_vurderinger_id") }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        hentHvisEksisterer(fraBehandling) ?: return

        connection.execute(
            """
            INSERT INTO krav_grunnlag (behandling_id, krav_vurderinger_id)
            SELECT ?, krav_vurderinger_id
            FROM krav_grunnlag
            WHERE behandling_id = ? AND aktiv = TRUE
            """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, fraBehandling.id)
            }
        }
    }
}
