package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.arena.ArenaOppslagGateway
import no.nav.aap.behandlingsflyt.arena.ArenaSykdomsvurderingResponse
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class ArenaMigreringService(
    private val arenaMigreringRepository: ArenaMigreringRepository,
    private val arenaOppslagGateway: ArenaOppslagGateway
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        arenaMigreringRepository = repositoryProvider.provide(),
        arenaOppslagGateway = gatewayProvider.provide(),
    )

    fun hentSaksnummerArena(sakId: SakId): String {
        val arenaMigrering = arenaMigreringRepository.hentForSakHvisEksisterer(sakId)
        requireNotNull(arenaMigrering) {
            "Fant ingen arenamigrering for sak $sakId, kan ikke migrere lovvalg og medlemskap."
        }
        return arenaMigrering.saksnummerArena
    }

    fun hentSykdomsvurdering(sakId: SakId): ArenaSykdomsvurderingResponse {
        val saksnummerArena = hentSaksnummerArena(sakId)
        val sykdomsvurderingFraArena = arenaOppslagGateway.hentSykdomsvurdering(saksnummerArena)

        requireNotNull(sykdomsvurderingFraArena) {
            "Kan ikke migrere sykdomsvurdering fra Arena for sak $sakId fordi det ikke finnes en vurdering for ordinær AAP"
        }

        // TODO lagre ned

        return sykdomsvurderingFraArena
    }
}