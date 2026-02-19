package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.OpphevetStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansEllerOpphørVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

class StansOpphørRepositoryImpl(
    private val connection: DBConnection,
) : StansOpphørRepository {
    override fun hentHvisEksisterer(behandlingId: BehandlingId): StansOpphørGrunnlag? {
        val vurderingerId: Long = connection.queryFirstOrNull(
            """
            select vurderinger_id from stans_opphor_grunnlag
            where behandling_id = ? and aktiv
        """
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper { row ->
                row.getLong("vurderinger_id")
            }
        } ?: return null


        return StansOpphørGrunnlag(
            stansOgOpphør = hentVurderinger(vurderingerId)
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
                            Vedtakstype.STANS -> Stans(
                                row.getArray("avslagsaarsaker", String::class).map { Avslagsårsak.valueOf(it) }.toSet()
                            )

                            Vedtakstype.OPPHØR -> Opphør(
                                row.getArray("avslagsaarsaker", String::class).map { Avslagsårsak.valueOf(it) }.toSet()
                            )
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

    private enum class Vedtaksstatus { GJELDENDE, OPPHEVET }
    private enum class Vedtakstype { STANS, OPPHØR }

    override fun lagre(
        behandlingId: BehandlingId,
        grunnlag: StansOpphørGrunnlag
    ) {
        val vurderingerId = connection.executeReturnKey(
            """
            insert into stans_opphor_vurderinger (opprettet_tid)  values (?)
        """
        ) {
            setParams {
                setInstant(1, Instant.now())
            }
        }
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
            insert into stans_opphor_grunnlag (behandling_id, vurderinger_id, opprettet_tid, aktiv) values (?, ?, ?, true)
        """
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingerId)
                setInstant(3, Instant.now())
            }
        }

        connection.executeBatch(
            """
            insert into stans_opphor_vurdering (vurderinger_id, opprettet_tid, fom, vurdert_i_behandling, vedtaksstatus, vedtakstype, avslagsaarsaker) values (
                ?, ?, ?, ?, ?, ?, ?
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
        TODO("Not yet implemented")
    }

    companion object : RepositoryFactory<StansOpphørRepository> {
        override fun konstruer(connection: DBConnection) = StansOpphørRepositoryImpl(connection)
    }
}