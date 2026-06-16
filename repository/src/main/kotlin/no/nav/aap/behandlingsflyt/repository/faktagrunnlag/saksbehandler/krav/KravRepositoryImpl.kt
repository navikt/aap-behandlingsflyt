package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Gjenopptak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Klage
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Tilleggsopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.TrukketSøknad
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory
import java.time.Instant

class KravRepositoryImpl(private val connection: DBConnection) : KravRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<KravRepositoryImpl> {
        override fun konstruer(connection: DBConnection): KravRepositoryImpl {
            return KravRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, vurderinger: Set<KravVurdering>) {
        val eksisterende = hentHvisEksisterer(behandlingId)
        val nytt = KravGrunnlag(vurderinger)

        if (eksisterende != nytt) {
            eksisterende?.let { deaktiverGrunnlag(behandlingId) }
            lagreGrunnlag(behandlingId, nytt)
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute(
            "UPDATE krav_grunnlag SET aktiv = FALSE WHERE behandling_id = ? AND aktiv"
        ) {
            setParams { setLong(1, behandlingId.id) }
            setResultValidator { require(it == 1) }
        }
    }

    private fun lagreGrunnlag(behandlingId: BehandlingId, grunnlag: KravGrunnlag) {
        val vurderingerId = connection.executeReturnKey("INSERT INTO krav_vurderinger (opprettet_tid) values (?)") {
            setParams { setInstant(1, Instant.now()) }
        }

        connection.executeBatch(
            """
            INSERT INTO krav_vurdering (
                krav_vurderinger_id, 
                journalpost_id, vurdert_av, opprettet_tid,
                begrunnelse, vurdert_i_behandling,
                krav_type, soknadsdato, soknadsdato_aarsak,
                overstyr_mulig_rett_fra, overstyr_mulig_rett_fra_aarsak,
                mulig_rett_fra, referanse
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            grunnlag.vurderinger
        ) {
            setParams { v ->
                setLong(1, vurderingerId)
                setString(2, v.journalpostId.identifikator)
                setString(3, v.vurdertAv)
                setInstant(4, v.opprettet)
                setString(5, v.begrunnelse)
                setLong(6, v.vurdertIBehandling.id)
                setUUID(13, v.referanse)
                when (v) {
                    is NyttKrav -> {
                        setEnumName(7, KravType.NYTT_KRAV_AAP)
                        setLocalDate(8, v.søknadsdato.dato)
                        setEnumName(9, v.søknadsdato.årsak)
                        setLocalDate(10, v.overstyrMuligRettFra?.dato)
                        setEnumName(11, v.overstyrMuligRettFra?.årsak)
                        setLocalDate(12, v.muligRettFra)
                    }

                    is Gjenopptak -> {
                        setEnumName(7, KravType.GJENOPPTAK)
                        setLocalDate(8, v.søknadsdato.dato)
                        setEnumName(9, v.søknadsdato.årsak)
                        setLocalDate(10, v.overstyrMuligRettFra?.dato)
                        setEnumName(11, v.overstyrMuligRettFra?.årsak)
                        setLocalDate(12, v.muligRettFra)
                    }

                    is TrukketSøknad -> {
                        setEnumName(7, KravType.TRUKKET_SØKNAD)
                        setLocalDate(8, null)
                        setEnumName(9, null as Enum<*>?)
                        setLocalDate(10, null)
                        setEnumName(11, null as Enum<*>?)
                        setLocalDate(12, null)
                        setUUID(13, v.referanse)
                    }

                    is Klage -> {
                        setEnumName(7, KravType.KLAGE)
                        setLocalDate(8, null)
                        setEnumName(9, null as Enum<*>?)
                        setLocalDate(10, null)
                        setEnumName(11, null as Enum<*>?)
                        setLocalDate(12, null)
                    }

                    is Tilleggsopplysning -> {
                        setEnumName(7, KravType.TILLEGGSOPPLYSNING)
                        setLocalDate(8, null)
                        setEnumName(9, null as Enum<*>?)
                        setLocalDate(10, null)
                        setEnumName(11, null as Enum<*>?)
                        setLocalDate(12, null)
                    }
                }
            }
        }

        connection.execute(
            "INSERT INTO krav_grunnlag (behandling_id, krav_vurderinger_id, opprettet_tid) VALUES (?, ?, ?)"
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingerId)
                setInstant(3, Instant.now())
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): KravGrunnlag? {
        return connection.queryFirstOrNull(
            "SELECT krav_vurderinger_id FROM krav_grunnlag WHERE behandling_id = ? AND aktiv"
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

    private fun hentVurderinger(vurderingerId: Long): Set<KravVurdering> {
        return connection.querySet(
            """
            SELECT referanse, journalpost_id, vurdert_av, krav_type,
                   soknadsdato, soknadsdato_aarsak,
                   overstyr_mulig_rett_fra, overstyr_mulig_rett_fra_aarsak,
                   begrunnelse, mulig_rett_fra, vurdert_i_behandling, opprettet_tid
            FROM krav_vurdering
            WHERE krav_vurderinger_id = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, vurderingerId) }
            setRowMapper(::mapVurdering)
        }
    }

    private fun mapVurdering(row: Row): KravVurdering {
        val referanse = row.getUUID("referanse")
        val journalpostId = JournalpostId(row.getString("journalpost_id"))
        val vurdertAv = row.getString("vurdert_av")
        val opprettet = row.getInstant("opprettet_tid")
        val begrunnelse = row.getString("begrunnelse")
        val vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling"))

        return when (val kravType = row.getEnum<KravType>("krav_type")) {
            KravType.NYTT_KRAV_AAP -> NyttKrav(
                referanse = referanse,
                journalpostId = journalpostId, vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
                søknadsdato = mapSøknadsdato(row),
                overstyrMuligRettFra = mapOverstyrMuligRettFra(row),
                muligRettFra = row.getLocalDate("mulig_rett_fra"),
            )

            KravType.GJENOPPTAK -> Gjenopptak(
                referanse = referanse,
                journalpostId = journalpostId, vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
                søknadsdato = mapSøknadsdato(row),
                overstyrMuligRettFra = mapOverstyrMuligRettFra(row),
                muligRettFra = row.getLocalDate("mulig_rett_fra"),
            )

            KravType.TRUKKET_SØKNAD -> TrukketSøknad(
                referanse = referanse,
                journalpostId = journalpostId, vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
            )

            KravType.KLAGE -> Klage(
                referanse = referanse,
                journalpostId = journalpostId, vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
            )

            KravType.TILLEGGSOPPLYSNING -> Tilleggsopplysning(
                referanse = referanse,
                journalpostId = journalpostId, vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
            )
        }
    }

    private fun mapOverstyrMuligRettFra(row: Row): OverstyrMuligRettFra? {
        return row.getLocalDateOrNull("overstyr_mulig_rett_fra")
            ?.let { OverstyrMuligRettFra(dato = it, årsak = row.getEnum("overstyr_mulig_rett_fra_aarsak")) }
    }

    private fun mapSøknadsdato(row: Row): Søknadsdato {
        return Søknadsdato(
            dato = row.getLocalDate("soknadsdato"),
            årsak = row.getEnum("soknadsdato_aarsak")
        )
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
            INSERT INTO krav_grunnlag (behandling_id, krav_vurderinger_id, opprettet_tid)
            SELECT ?, krav_vurderinger_id, ?
            FROM krav_grunnlag
            WHERE behandling_id = ? AND aktiv
            """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.id)
                setInstant(2, Instant.now())
                setLong(3, fraBehandling.id)
            }
        }
    }
}
