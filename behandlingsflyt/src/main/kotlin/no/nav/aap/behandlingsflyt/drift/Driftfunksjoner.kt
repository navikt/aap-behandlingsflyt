package no.nav.aap.behandlingsflyt.drift

import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prosessering.MeldeperiodeTilMeldekortBackendJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryProvider
import org.slf4j.LoggerFactory

/**
 * Klasse for alle driftsfunksjoner. Skal kún brukes av DriftApi.
 * */
class Driftfunksjoner(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider),
        sakOgBehandlingService = SakOgBehandlingService(repositoryProvider),
        sakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)

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

        val sak = sakOgBehandlingService.hentSakFor(behandlingId)

        prosesserBehandlingService.triggProsesserBehandling(
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

    fun resendMeldeperioderTilMeldekortBackend(saksnummer: Saksnummer) {
        val sak = sakRepository.hent(saksnummer)
        val behandling =
            behandlingRepository.finnSisteBehandlingFor(
                sakId = sak.id,
                behandlingstypeFilter = TypeBehandling.entries
            )
        val aktuelleBehandlingstyper = listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)

        when {
            behandling == null -> {
                log.info("Finner ikke behandling for sak ${sak.saksnummer}. Resender ikke meldeperioder til meldekort-backend.")
            }

            !aktuelleBehandlingstyper.contains(behandling.typeBehandling()) -> {
                log.info("Siste behandling ${behandling.referanse} for sak ${sak.saksnummer} er ikke av type " +
                        "${aktuelleBehandlingstyper.joinToString("eller")}. Resender ikke meldeperioder til meldekort-backend.")
            }

            behandling.status() != Status.AVSLUTTET -> {
                log.info(
                    "Siste behandling ${behandling.referanse} for sak ${sak.saksnummer} er ikke avsluttet. " +
                            "Resender ikke meldeperioder til meldekort-backend."
                )
            }

            else -> {
                log.info("Resender meldeperioder til meldekort-backend for sak ${sak.saksnummer} og behandling ${behandling.referanse}")
                MeldeperiodeTilMeldekortBackendJobbUtfører.nyJobb(behandling.sakId, behandling.id)
            }
        }
    }
}
