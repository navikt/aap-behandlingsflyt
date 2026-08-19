package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakMedVedtakResponse
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class ArenaMigreringService(
    private val apiInternGateway: ApiInternGateway,
    private val arenaMigreringRepository: ArenaMigreringRepository,
) {
    constructor(
        gatewayProvider: GatewayProvider,
        repositoryProvider: RepositoryProvider
    ) : this(
        gatewayProvider.provide<ApiInternGateway>(),
        repositoryProvider.provide<ArenaMigreringRepository>()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Henter Arena-saken med tilhørende vedtak fra api-intern og lagrer den som JSON på migreringen.
     * Krever at det allerede finnes en arenamigrering for saken.
     */
    fun hentOgLagreArenaSakMedVedtak(sakId: SakId, saksnummerArena: String): ArenaSakMedVedtakResponse {
        val arenaSakMedVedtak = apiInternGateway.hentArenaSakMedVedtak(saksnummerArena)

        arenaMigreringRepository.lagreArenaSakData(sakId, arenaSakMedVedtak)
        log.info("Lagret arenasak med ${arenaSakMedVedtak.vedtak.size} vedtak for sak $sakId")

        return arenaSakMedVedtak
    }
}