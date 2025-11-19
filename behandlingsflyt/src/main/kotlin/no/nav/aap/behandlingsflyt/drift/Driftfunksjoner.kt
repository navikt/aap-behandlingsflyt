package no.nav.aap.behandlingsflyt.drift

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider

/**
 * Klasse for alle driftsfunksjoner. Skal kún brukes av DriftApi.
 * */
class Driftfunksjoner(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakService: SakService,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
        sakService = SakService(repositoryProvider)
    )

    fun flyttBehandlingTilSteg(behandlingId: BehandlingId, connection: DBConnection, stegType: StegType) {

        // valider at stegtype er FØR gjeldende steg
        // Kun lov å flyutte til et steg som er i "utredes"-fasen

        val query = """
            UPDATE STEG_HISTORIKK
            SET aktiv = false
            WHERE behandling_id = ?
              AND aktiv = true
              AND EXISTS (
                  SELECT 1 
                  FROM behandling 
                  WHERE behandling.id = ?
                    AND behandling.status IN ('UTREDES')
              );
            INSERT INTO STEG_HISTORIKK (behandling_id, steg, status)
                SELECT id, ?, 'START'
                FROM behandling
                WHERE id = ?
                    AND status IN ('UTREDES');
        """.trimIndent()

        val sak = sakService.hentSakFor(behandlingId)

        prosesserBehandlingService.triggProsesserBehandling(
            sakId = sak.id,
            behandlingId = behandlingId,
        )

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, behandlingId.toLong())
                setEnumName(3, stegType)
                setLong(4, behandlingId.toLong())
            }
        }
    }
}