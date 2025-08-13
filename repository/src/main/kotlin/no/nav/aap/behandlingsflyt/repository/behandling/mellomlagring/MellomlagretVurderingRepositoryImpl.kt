package no.nav.aap.behandlingsflyt.repository.behandling.mellomlagring

import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurdering
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class MellomlagretVurderingRepositoryImpl(private val connection: DBConnection) : MellomlagretVurderingRepository {
    companion object : Factory<MellomlagretVurderingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): MellomlagretVurderingRepositoryImpl {
            return MellomlagretVurderingRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(
        behandlingId: BehandlingId,
        avklaringsbehovKode: String
    ): MellomlagretVurdering? {
        return connection.queryFirstOrNull(
            """
            SELECT * FROM MELLOMLAGRET_VURDERING WHERE behandling_id = ? AND avklaringsbehov_kode = ?;
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setString(2, avklaringsbehovKode)
            }
            setRowMapper { row ->
                MellomlagretVurdering(
                    behandlingId = BehandlingId(row.getLong("behandling_id")),
                    avklaringsbehovKode = row.getString("avklaringsbehov_kode"),
                    data = row.getString("data"),
                    vurdertAv = row.getString("vurdert_av"),
                    vurdertDato = row.getLocalDateTime("vurdert_dato"),
                )
            }
        }
    }

    override fun lagre(mellomlagretVurdering: MellomlagretVurdering) {

        connection.execute(
            """ 
            DELETE FROM MELLOMLAGRET_VURDERING WHERE behandling_id = ? AND avklaringsbehov_kode = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, mellomlagretVurdering.behandlingId.id)
                setString(2, mellomlagretVurdering.avklaringsbehovKode)
            }
        }
        connection.execute(
            """
            INSERT INTO MELLOMLAGRET_VURDERING (behandling_id, avklaringsbehov_kode, data, vurdert_av, vurdert_dato)
            VALUES (?, ?, ?::jsonb, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, mellomlagretVurdering.behandlingId.id)
                setString(2, mellomlagretVurdering.avklaringsbehovKode)
                setString(3, mellomlagretVurdering.data)
                setString(4, mellomlagretVurdering.vurdertAv)
                setLocalDateTime(5, mellomlagretVurdering.vurdertDato)
            }
        }

    }

}