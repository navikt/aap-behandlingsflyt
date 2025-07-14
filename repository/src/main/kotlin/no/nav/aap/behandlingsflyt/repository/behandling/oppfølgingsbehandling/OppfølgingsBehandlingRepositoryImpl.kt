package no.nav.aap.behandlingsflyt.repository.behandling.oppfølgingsbehandling

import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsBehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsoppgaveGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class OppfølgingsBehandlingRepositoryImpl(private val connection: DBConnection) : OppfølgingsBehandlingRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<OppfølgingsBehandlingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): OppfølgingsBehandlingRepositoryImpl {
            return OppfølgingsBehandlingRepositoryImpl(connection)
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        grunnlag: OppfølgingsoppgaveGrunnlag
    ) {
        val vurderingInsert = """
            INSERT INTO OPPFOLGINGSOPPGAVE_VURDERING (konsekvens_av_oppfolging, opplysninger_til_revurdering,
                                                      aarsak_til_revurdering, vurdert_av)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        val key = connection.executeReturnKey(vurderingInsert) {
            setParams {
                setEnumName(1, grunnlag.konsekvensAvOppfølging)
                setArray(2, grunnlag.opplysningerTilRevurdering.map { it.name })
                setString(3, grunnlag.årsak)
                setString(4, grunnlag.vurdertAv)
            }
        }

        val query = """
            INSERT INTO OPPFOLGINGSOPPGAVE_GRUNNLAG (behandling_id, vurdering_id) VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, key)
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): OppfølgingsoppgaveGrunnlag? {
        val query = """
            SELECT *
            FROM OPPFOLGINGSOPPGAVE_GRUNNLAG og
                     JOIN OPPFOLGINGSOPPGAVE_VURDERING ov ON og.vurdering_id = ov.id
            WHERE behandling_id = ? AND og.aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper {
                OppfølgingsoppgaveGrunnlag(
                    konsekvensAvOppfølging = it.getEnum("konsekvens_av_oppfolging"),
                    opplysningerTilRevurdering = it.getArray("opplysninger_til_revurdering", String::class).map(
                        ÅrsakTilBehandling::valueOf
                    ),
                    årsak = it.getString("aarsak_til_revurdering"),
                    vurdertAv = it.getString("vurdert_av"),
                )
            }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
    }

    override fun slett(behandlingId: BehandlingId) {

    }
}