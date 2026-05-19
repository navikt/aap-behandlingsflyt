package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.avbrytaktivitetspliktbehandling

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.Factory

class AvbrytAktivitetspliktbehandlingRepositoryImpl(
    private val connection: DBConnection
) : AvbrytAktivitetspliktbehandlingRepository {
    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: AvbrytAktivitetspliktbehandlingVurdering
    ) {
        deaktiverEksisterendeGrunnlag(behandlingId)
        val vurderingId = lagreVurdering(vurdering)
        lagreGrunnlag(behandlingId, vurderingId)
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): AvbrytAktivitetspliktbehandlingGrunnlag? {
        return connection.queryFirstOrNull<AvbrytAktivitetspliktbehandlingGrunnlag>(
            """
                select *
                from avbryt_aktivitetspliktbehandling_grunnlag as grunnlag
                left join avbryt_aktivitetspliktbehandling_vurdering as vurdering on grunnlag.id = vurdering.id
                where grunnlag.aktiv = true and grunnlag.behandling_id = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                AvbrytAktivitetspliktbehandlingGrunnlag(
                    vurdering = AvbrytAktivitetspliktbehandlingVurdering(
                        årsak = it.getEnum("aarsak"),
                        begrunnelse = it.getString("begrunnelse"),
                        vurdertAv = Bruker(ident = it.getString("vurdert_av")),
                        opprettetTidspunkt = it.getLocalDateTime("opprettet_tid"),
                    )
                )
            }
        }
    }

    private fun deaktiverEksisterendeGrunnlag(behandlingId: BehandlingId) {
        return connection.execute(
            """
            update avbryt_aktivitetspliktbehandling_grunnlag
            set aktiv = false
            where behandling_id = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }

    private fun lagreVurdering(vurdering: AvbrytAktivitetspliktbehandlingVurdering): Long {
        return connection.executeReturnKey(
            """
            insert into avbryt_aktivitetspliktbehandling_vurdering (aarsak, begrunnelse, vurdert_av)
            values (?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setEnumName(1, vurdering.årsak)
                setString(2, vurdering.begrunnelse)
                setString(3, vurdering.vurdertAv.ident)
            }
        }
    }

    private fun lagreGrunnlag(behandlingId: BehandlingId, vurderingId: Long) {
        connection.executeReturnKey(
            """
            insert into avbryt_aktivitetspliktbehandling_grunnlag(
                BEHANDLING_ID, VURDERING_ID, AKTIV
            ) values (?, ?, TRUE)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        // Skal ikke gjøres
    }

    override fun slett(behandlingId: BehandlingId) {
        // Skal ikke gjøres
    }

    companion object : Factory<AvbrytAktivitetspliktbehandlingRepository> {
        override fun konstruer(connection: DBConnection): AvbrytAktivitetspliktbehandlingRepository {
            return AvbrytAktivitetspliktbehandlingRepositoryImpl(connection)
        }
    }
}