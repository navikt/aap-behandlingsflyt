package no.nav.aap.behandlingsflyt.repository.behandling.vedtak

import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import java.time.LocalDate
import java.time.LocalDateTime

class VedtakRepositoryImpl(private val connection: DBConnection) : VedtakRepository {

    companion object : Factory<VedtakRepositoryImpl> {
        override fun konstruer(connection: DBConnection): VedtakRepositoryImpl {
            return VedtakRepositoryImpl(connection)
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vedtakstidspunkt: LocalDateTime,
        virkningstidspunkt: LocalDate?,
    ) {
        connection.execute(
            """
            INSERT INTO VEDTAK (behandling_id, vedtakstidspunkt, virkningstidspunkt) VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLocalDateTime(2, vedtakstidspunkt)
                setLocalDate(3, virkningstidspunkt)
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
                    vedtakstidspunkt = it.getLocalDateTime("vedtakstidspunkt"),
                    virkningstidspunkt = it.getLocalDateOrNull("virkningstidspunkt")
                )
            }
        }
    }

    override fun hentId(behandlingId: BehandlingId): Long {
        return requireNotNull(connection.queryFirstOrNull("SELECT * FROM VEDTAK WHERE behandling_id = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                    it.getLong("ID")
            }
        })
    }

    override fun slett(behandlingId: BehandlingId) {
       val vedtak = hent(behandlingId)
        check(vedtak == null) {
            "Fant vedtak med id $behandlingId på trukket søknad"
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Denne trengs ikke implementeres
    }
}
