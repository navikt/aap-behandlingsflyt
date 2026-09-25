package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.ArenaMigreringRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Instant

class ArenaMigreringService(
    private val arenaMigreringRepository: ArenaMigreringRepository,
    private val arenaMigreringsdataRepository: ArenaMigreringsdataRepository,
    private val arenaOppslagGateway: ArenaOppslagGateway,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        arenaMigreringRepository = repositoryProvider.provide(),
        arenaMigreringsdataRepository = repositoryProvider.provide(),
        arenaOppslagGateway = gatewayProvider.provide(),
    )

    fun hentSaksnummerArena(sakId: SakId): String {
        val arenaMigrering = arenaMigreringRepository.hentForSakHvisEksisterer(sakId)
        requireNotNull(arenaMigrering) {
            "Fant ingen migrering for sak $sakId."
        }
        return arenaMigrering.saksnummerArena
    }

    fun hentSykdomsvurdering(sakId: SakId): ArenaSykdomsvurderingResponse {
        logger.info("Henter sykdomsvurdering fra Arena for sak $sakId.")
        val saksnummerArena = hentSaksnummerArena(sakId)
        val sykdomsvurderingFraArena = arenaOppslagGateway.hentSykdomsvurdering(saksnummerArena)

        requireNotNull(sykdomsvurderingFraArena) {
            "Kan ikke migrere sykdomsvurdering fra Arena for sak $sakId fordi det ikke finnes en vurdering for ordinær AAP"
        }

        return sykdomsvurderingFraArena
    }

    fun lagreMigreringsdataForSporing(behandlingId: BehandlingId, steg: StegType, data: Any) {
        logger.info("Lagrer migreringsdata for behandling $behandlingId, steg $steg.")
        arenaMigreringsdataRepository.lagre(
            behandlingId = behandlingId,
            steg = steg,
            data = data,
            hentetTidspunkt = Instant.now(),
        )
    }
}