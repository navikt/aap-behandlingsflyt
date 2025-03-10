package no.nav.aap.behandlingsflyt.repository.behandling.vedtak

import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import java.time.LocalDateTime

class VedtakRepositoryImpl(private val connection: DBConnection) : VedtakRepository {

    companion object : Factory<VedtakRepositoryImpl> {
        override fun konstruer(connection: DBConnection): VedtakRepositoryImpl {
            return VedtakRepositoryImpl(connection)
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vedtakstidspunkt: LocalDateTime
    ) {
        connection.execute(
            """
            INSERT INTO VEDTAK (behandling_id, vedtakstidspunkt) VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLocalDateTime(2, vedtakstidspunkt)
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): Vedtak? {
        return connection.queryFirstOrNull("SELECT * FROM VEDTAK WHERE behandling_id = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                Vedtak(
                    behandlingId = BehandlingId(it.getLong("behandling_id")),
                    vedtakstidspunkt = it.getLocalDateTime("vedtakstidspunkt")
                )
            }
        }
    }
}
