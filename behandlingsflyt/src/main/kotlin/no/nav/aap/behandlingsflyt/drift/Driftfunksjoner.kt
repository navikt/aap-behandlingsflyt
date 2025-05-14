package no.nav.aap.behandlingsflyt.drift

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepositoryImpl

/**
 * Klasse for alle driftsfunksjoner. Skal kún brukes av DriftApi.
 * */
class Driftfunksjoner(
    private val repositoryRegistry: RepositoryRegistry,
) {

    fun flyttBehandlingTilStart(behandlingId: BehandlingId, connection: DBConnection) {
        val query = """
            UPDATE STEG_HISTORIKK
            SET aktiv = false
            WHERE behandling_id = ?
              AND aktiv = true
              AND EXISTS (
                  SELECT 1 
                  FROM behandling 
                  WHERE behandling.id = ?
                    AND behandling.status IN ('UTREDES', 'IVERKSETTES')
              );
            INSERT INTO STEG_HISTORIKK (behandling_id, steg, status)
            SELECT id, 'START_BEHANDLING', 'START'
            FROM behandling
            WHERE id = ?
              AND status IN ('UTREDES', 'IVERKSETTES');
        """.trimIndent()

        val repositoryProvider = repositoryRegistry.provider(connection)
        val sakOgBehandlingService = SakOgBehandlingService(repositoryProvider)

        val sak = sakOgBehandlingService.hentSakFor(behandlingId)

        ProsesserBehandlingService(FlytJobbRepositoryImpl(connection)).triggProsesserBehandling(
            sakId = sak.id,
            behandlingId = behandlingId,
        )

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, behandlingId.toLong())
                setLong(3, behandlingId.toLong())
            }
        }
    }
}