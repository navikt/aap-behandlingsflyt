package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.IkkeStansOpphørVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.OpphevetStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.OpphørVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansEllerOpphørVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.repository.RepositoryFactory
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class StansOpphørRepositoryImpl(
    private val connection: DBConnection,
) : StansOpphørRepository {
    override fun hentHvisEksisterer(behandlingId: BehandlingId): StansOpphørGrunnlag? {
        val (vurderingerId, vurderingerIdV2, stansOpphørSetId) = connection.queryFirstOrNull<List<Long?>>(
            """
            select stans_opphor_set_id, vurderinger_id, vurderinger_id_v2
            from stans_opphor_grunnlag
            where behandling_id = ? and aktiv
            limit 1
        """
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper { row ->
                listOf(
                    row.getLong("vurderinger_id"),
                    row.getLongOrNull("vurderinger_id_v2"),
                    row.getLongOrNull("stans_opphor_set_id"),
                )
            }
        } ?: return null


        return StansOpphørGrunnlag(
            stansOgOpphør = hentVurderinger(vurderingerId!!),
            stansOpphørV2 = hentStansOpphør(stansOpphørSetId),
            stansOpphørVurderingerV2 = hentVurderingerV2(vurderingerIdV2),
        )
    }

    private fun hentVurderinger(vurderingerId: Long): Set<StansEllerOpphørVurdering> {
        return connection.querySet(
            """
            select * from stans_opphor_vurdering
            where stans_opphor_vurdering.vurderinger_id = ?
        """
        ) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper { row ->
                val type = row.getEnum<Vedtaksstatus>("vedtaksstatus")
                when (type) {
                    Vedtaksstatus.GJELDENDE -> GjeldendeStansEllerOpphør(
                        fom = row.getLocalDate("fom"),
                        opprettet = row.getInstant("opprettet_tid"),
                        vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
                        vurdering = when (row.getEnum<Vedtakstype>("vedtakstype")) {
                            Vedtakstype.STANS -> Stans(avslagsårsaks(row))
                            Vedtakstype.OPPHØR -> Opphør(avslagsårsaks(row))
                        }
                    )

                    Vedtaksstatus.OPPHEVET -> OpphevetStansEllerOpphør(
                        fom = row.getLocalDate("fom"),
                        opprettet = row.getInstant("opprettet_tid"),
                        vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
                    )
                }
            }
        }
    }

    private fun hentStansOpphør(stansOpphørSetId: Long?): Map<LocalDate, StansEllerOpphør>? {
        if (stansOpphørSetId == null) {
            return null
        }

        return connection.querySet(
            """
            select fom, avslagsaarsaker, vurdering
            from stans_opphor
            where stans_opphor_set_id = ?
            """
        ) {
            setParams {
                setLong(1, stansOpphørSetId)
            }
            setRowMapper { row ->
                row.getLocalDate("fom") to when (row.getEnum<Vedtakstype>("vurdering")) {
                    Vedtakstype.STANS -> Stans(avslagsårsaks(row))
                    Vedtakstype.OPPHØR -> Opphør(avslagsårsaks(row))
                }
            }
        }.toMap()
    }

    private fun avslagsårsaks(row: Row): Set<Avslagsårsak> =
        row.getArray("avslagsaarsaker", String::class).map { Avslagsårsak.valueOf(it) }.toSet()

    private fun hentVurderingerV2(vurderingerV2Id: Long?): Set<StansOpphørVurdering>? {
        if (vurderingerV2Id == null) {
            return null
        }
        return connection.querySet(
            """
            select fom, avslagsaarsaker, vurdering, vurdert_i_behandling, vurdert_tidspunkt
            from stans_opphor_vurdering_v2
            where vurderinger_id = ?
            """
        ) {
            setParams {
                setLong(1, vurderingerV2Id)
            }
            setRowMapper { row ->
                when (row.getEnum<Vurderingtype>("vurdering")) {
                    Vurderingtype.STANS -> StansVurdering(
                        fom = row.getLocalDate("fom"),
                        årsaker = avslagsårsaks(row),
                        vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
                        vurdertTidspunkt = row.getInstant("vurdert_tidspunkt"),
                    )

                    Vurderingtype.OPPHØR -> OpphørVurdering(
                        fom = row.getLocalDate("fom"),
                        årsaker = avslagsårsaks(row),
                        vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
                        vurdertTidspunkt = row.getInstant("vurdert_tidspunkt"),
                    )

                    Vurderingtype.IKKE_STANS_OPPHØR -> IkkeStansOpphørVurdering(
                        fom = row.getLocalDate("fom"),
                        vurdertIBehandling = BehandlingId(row.getLong("vurdert_i_behandling")),
                        vurdertTidspunkt = row.getInstant("vurdert_tidspunkt"),
                    )
                }
            }
        }
    }

    private enum class Vedtaksstatus { GJELDENDE, OPPHEVET }
    private enum class Vedtakstype { STANS, OPPHØR }
    private enum class Vurderingtype { STANS, OPPHØR, IKKE_STANS_OPPHØR }

    override fun lagre(
        behandlingId: BehandlingId,
        grunnlag: StansOpphørGrunnlag
    ) {
        val vurderingerId = lagreVurderinger(grunnlag)
        val vurderingerV2Id = lagreVurderingerV2(grunnlag)
        val stansOpphørSetId = lagreStansOpphør(grunnlag)

        connection.execute(
            """
            update stans_opphor_grunnlag
            set aktiv = false
            where behandling_id = ?
        """
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
        }

        connection.execute(
            """
            insert into stans_opphor_grunnlag (behandling_id, vurderinger_id, vurderinger_id_v2, stans_opphor_set_id, opprettet_tid, aktiv) values (?, ?, ?, ?, ?, true)
        """
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingerId)
                setLong(3, vurderingerV2Id)
                setLong(4, stansOpphørSetId)
                setInstant(5, Instant.now())
            }
        }
    }

    private fun lagreStansOpphør(grunnlag: StansOpphørGrunnlag): Long? {
        val stansOpphørV2 = grunnlag.stansOpphørV2 ?: return null

        val stansOpphørSetId = connection.executeReturnKey(
            """
                    insert into stans_opphor_set (opprettet_tid)  values (?)
                """
        ) {
            setParams {
                setInstant(1, Instant.now())
            }
        }

        connection.executeBatch("""
            insert into stans_opphor (fom, vurdering, avslagsaarsaker, stans_opphor_set_id) values (?, ?, ?::text[], ?)
        """.trimIndent(), stansOpphørV2.entries,
        ) {
            setParams { (fom, stansEllerOpphør) ->
                setLocalDate(1, fom)
                setEnumName(2, when (stansEllerOpphør) {
                    is Opphør-> Vedtakstype.OPPHØR
                    is Stans -> Vedtakstype.STANS
                })
                setArray(3, stansEllerOpphør.årsaker.map { it.name })
                setLong(4, stansOpphørSetId)
            }
        }

        return stansOpphørSetId
    }

    private fun lagreVurderinger(grunnlag: StansOpphørGrunnlag): Long {
        val vurderingerId =  connection.executeReturnKey(
            """
                    insert into stans_opphor_vurderinger (opprettet_tid)  values (?)
                """
        ) {
            setParams {
                setInstant(1, Instant.now())
            }
        }

        connection.executeBatch(
            """
            insert into stans_opphor_vurdering (vurderinger_id, opprettet_tid, fom, vurdert_i_behandling, vedtaksstatus, vedtakstype, avslagsaarsaker) values (
                ?, ?, ?, ?, ?, ?, ?::text[]
            )
        """, grunnlag.stansOgOpphør
        ) {
            setParams {
                setLong(1, vurderingerId)
                setInstant(2, it.opprettet.truncatedTo(ChronoUnit.MILLIS))
                setLocalDate(3, it.fom)
                setLong(4, it.vurdertIBehandling.id)
                when (it) {
                    is GjeldendeStansEllerOpphør -> {
                        setEnumName(5, Vedtaksstatus.GJELDENDE)
                        when (it.vurdering) {
                            is Stans -> {
                                setEnumName(6, Vedtakstype.STANS)
                                setArray(7, it.vurdering.årsaker.map { it.name })
                            }

                            is Opphør -> {
                                setEnumName(6, Vedtakstype.OPPHØR)
                                setArray(7, it.vurdering.årsaker.map { it.name })
                            }
                        }
                    }

                    is OpphevetStansEllerOpphør -> {
                        setEnumName(5, Vedtaksstatus.OPPHEVET)
                        setString(6, null)
                        setArray(7, emptyList())
                    }
                }
            }
        }

        return vurderingerId
    }

    private fun lagreVurderingerV2(grunnlag: StansOpphørGrunnlag): Long? {
        val stansOpphørVurderingerV2 = grunnlag.stansOpphørVurderingerV2 ?: return null

        val stansOpphørVurderingerIdV2 =  connection.executeReturnKey(
            """
                    insert into stans_opphor_vurderinger_v2 (opprettet_tid)  values (?)
                """
        ) {
            setParams {
                setInstant(1, Instant.now())
            }
        }

        connection.executeBatch("""
            insert into stans_opphor_vurdering_v2
            (vurderinger_id, vurdert_tidspunkt, vurdert_i_behandling, fom, vurdering, avslagsaarsaker)
            values (?, ?, ?, ?, ?, ?::text[])
            """, stansOpphørVurderingerV2
        ) {
            setParams {
                setLong(1, stansOpphørVurderingerIdV2)
                setInstant(2, it.vurdertTidspunkt)
                setLong(3, it.vurdertIBehandling.id)
                setLocalDate(4, it.fom)
                when (it) {
                    is OpphørVurdering -> {
                        setEnumName(5, Vurderingtype.OPPHØR)
                        setArray(6, it.årsaker.map { it.name })
                    }
                    is StansVurdering -> {
                        setEnumName(5, Vurderingtype.STANS)
                        setArray(6, it.årsaker.map { it.name })
                    }
                    is IkkeStansOpphørVurdering-> {
                        setEnumName(5, Vurderingtype.IKKE_STANS_OPPHØR)
                        setArray(6, emptyList())
                    }
                }
            }
        }

        return stansOpphørVurderingerIdV2
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        hentHvisEksisterer(fraBehandling)?.let {
            lagre(tilBehandling, it)
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        connection.execute(
            """
        WITH grunnlag AS (
            SELECT vurderinger_id
            FROM stans_opphor_grunnlag
            WHERE behandling_id = ?
        ),
        deleted_vurdering AS (
            DELETE FROM stans_opphor_vurdering
            WHERE vurderinger_id IN (SELECT vurderinger_id FROM grunnlag)
        ),
        deleted_grunnlag AS (
            DELETE FROM stans_opphor_grunnlag
            WHERE behandling_id = ?
        )
        DELETE FROM stans_opphor_vurderinger
        WHERE id IN (SELECT vurderinger_id FROM grunnlag)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, behandlingId.id)
            }
        }
    }

    companion object : RepositoryFactory<StansOpphørRepository> {
        override fun konstruer(connection: DBConnection) = StansOpphørRepositoryImpl(connection)
    }
}