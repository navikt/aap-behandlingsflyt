package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Klage
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertRettighetstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Tilleggsopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.TrukketSøknad
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.verdityper.Bruker
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
                krav_type, soknadsdato, soknadsdato_aarsak, soknadsdato_begrunnelse,
                overstyr_mulig_rett_fra, overstyr_mulig_rett_fra_aarsak, overstyr_mulig_rett_fra_begrunnelse,
                mulig_rett_fra, referanse, arena_saksnummer, rettighetstype, resterende_kvote_ordinaer,
                virkningstidspunkt_arena
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            grunnlag.vurderinger
        ) {
            setParams { v ->
                setLong(1, vurderingerId)
                setString(2, v.journalpostId?.identifikator)
                setString(3, v.vurdertAv.ident)
                setInstant(4, v.opprettet)
                setString(5, v.begrunnelse)
                setLong(6, v.vurdertIBehandling.id)
                setUUID(15, v.referanse.verdi)

                // Feltene under gjelder kun MigrertKrav. Settes til null som standard,
                // slik at hver enkelt when-gren slipper å gjenta dette.
                setString(16, null)
                setString(17, null)
                setInt(18, null)
                setLocalDate(19, null)

                when (v) {
                    is RelevantKrav -> {
                        setEnumName(7, KravType.RELEVANT_KRAV)
                        setLocalDate(8, v.søknadsdato.dato)
                        setEnumName(9, v.søknadsdato.årsak)
                        setString(10, v.søknadsdato.begrunnelse)
                        setLocalDate(11, v.overstyrMuligRettFra?.dato)
                        setEnumName(12, v.overstyrMuligRettFra?.årsak)
                        setString(13, v.overstyrMuligRettFra?.begrunnelse)
                        setLocalDate(14, v.muligRettFra)
                    }

                    is TrukketSøknad -> {
                        setEnumName(7, KravType.TRUKKET_SØKNAD)
                        setLocalDate(8, null)
                        setEnumName(9, null as Enum<*>?)
                        setString(10, null)
                        setLocalDate(11, null)
                        setEnumName(12, null as Enum<*>?)
                        setString(13, null)
                        setLocalDate(14, null)
                    }

                    is Klage -> {
                        setEnumName(7, KravType.KLAGE)
                        setLocalDate(8, null)
                        setEnumName(9, null as Enum<*>?)
                        setString(10, null)
                        setLocalDate(11, null)
                        setEnumName(12, null as Enum<*>?)
                        setString(13, null)
                        setLocalDate(14, null)
                    }

                    is Tilleggsopplysning -> {
                        setEnumName(7, KravType.TILLEGGSOPPLYSNING)
                        setLocalDate(8, null)
                        setEnumName(9, null as Enum<*>?)
                        setString(10, null)
                        setLocalDate(11, null)
                        setEnumName(12, null as Enum<*>?)
                        setString(13, null)
                        setLocalDate(14, null)
                    }

                    is MigrertKrav -> {
                        setEnumName(7, KravType.MIGRERT_KRAV)
                        setLocalDate(8, null)
                        setEnumName(9, null as Enum<*>?)
                        setString(10, null)
                        setLocalDate(11, null)
                        setEnumName(12, null as Enum<*>?)
                        setString(13, null)
                        setLocalDate(14, v.muligRettFra)
                        setString(16, v.arenaSaksnummer)
                        setEnumName(17, v.rettighetstype)
                        setInt(18, v.resterendeKvoteOrdinaer)
                        setLocalDate(19, v.virkningstidspunktArena)
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
                   soknadsdato, soknadsdato_aarsak, soknadsdato_begrunnelse,
                   overstyr_mulig_rett_fra, overstyr_mulig_rett_fra_aarsak, overstyr_mulig_rett_fra_begrunnelse,
                   begrunnelse, mulig_rett_fra, vurdert_i_behandling, opprettet_tid,
                   arena_saksnummer, rettighetstype, resterende_kvote_ordinaer, virkningstidspunkt_arena
            FROM krav_vurdering
            WHERE krav_vurderinger_id = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, vurderingerId) }
            setRowMapper(::mapVurdering)
        }
    }

    private fun mapVurdering(row: Row): KravVurdering {
        val referanse = Kravreferanse(row.getUUID("referanse"))
        val journalpostId = row.getStringOrNull("journalpost_id")?.let(::JournalpostId)
        val vurdertAv = Bruker(row.getString("vurdert_av"))
        val opprettet = row.getInstant("opprettet_tid")
        val begrunnelse = row.getString("begrunnelse")
        val vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling"))

        return when (val kravType = row.getEnum<KravType>("krav_type")) {
            KravType.RELEVANT_KRAV -> RelevantKrav(
                referanse = referanse,
                journalpostId = kreverJournalpostId(journalpostId, kravType, referanse), vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
                søknadsdato = mapSøknadsdato(row),
                overstyrMuligRettFra = mapOverstyrMuligRettFra(row),
                muligRettFra = row.getLocalDate("mulig_rett_fra"),
            )
            
            KravType.TRUKKET_SØKNAD -> TrukketSøknad(
                referanse = referanse,
                journalpostId = kreverJournalpostId(journalpostId, kravType, referanse), vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
            )

            KravType.KLAGE -> Klage(
                referanse = referanse,
                journalpostId = kreverJournalpostId(journalpostId, kravType, referanse), vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
            )

            KravType.TILLEGGSOPPLYSNING -> Tilleggsopplysning(
                referanse = referanse,
                journalpostId = kreverJournalpostId(journalpostId, kravType, referanse), vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
            )

            KravType.MIGRERT_KRAV -> MigrertKrav(
                referanse = referanse,
                vurdertAv = vurdertAv,
                begrunnelse = begrunnelse,
                vurdertIBehandling = vurdertIBehandling, opprettet = opprettet,
                virkningstidspunktArena = row.getLocalDate("virkningstidspunkt_arena"),
                muligRettFra = row.getLocalDate("mulig_rett_fra"),
                arenaSaksnummer = row.getString("arena_saksnummer"),
                rettighetstype = row.getEnum<MigrertRettighetstype>("rettighetstype"),
                resterendeKvoteOrdinaer = row.getInt("resterende_kvote_ordinaer"),
            )
        }
    }

    private fun kreverJournalpostId(
        journalpostId: JournalpostId?,
        kravType: KravType,
        referanse: Kravreferanse
    ): JournalpostId {
        return requireNotNull(journalpostId) {
            "Mangler journalpost_id på krav_vurdering med krav_type=$kravType og referanse=${referanse.verdi}."
        }
    }

    private fun mapOverstyrMuligRettFra(row: Row): OverstyrMuligRettFra? {
        return row.getLocalDateOrNull("overstyr_mulig_rett_fra")
            ?.let {
                OverstyrMuligRettFra(
                    dato = it,
                    årsak = row.getEnum("overstyr_mulig_rett_fra_aarsak"),
                    begrunnelse = row.getStringOrNull("overstyr_mulig_rett_fra_begrunnelse") ?: "",
                )
            }
    }

    private fun mapSøknadsdato(row: Row): Søknadsdato {
        return Søknadsdato(
            dato = row.getLocalDate("soknadsdato"),
            årsak = row.getEnum("soknadsdato_aarsak"),
            begrunnelse = row.getStringOrNull("soknadsdato_begrunnelse") ?: "",
        )
    }

    override fun slett(behandlingId: BehandlingId) {
        // Skal ikke slette krav selv om søknad trekkes
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
