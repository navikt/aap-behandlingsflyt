package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaLovvalgOgMedlemskapResponse
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider


class ArenaMigreringService(
    private val arenaMigreringRepository: ArenaMigreringRepository,
    // TODO kalle arenaoppslag direkte for å slippe ekstra mapping?
    private val apiInternGateway: ApiInternGateway,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        arenaMigreringRepository = repositoryProvider.provide(),
        apiInternGateway = gatewayProvider.provide(),
    )

    fun hentSaksnummerArena(sakId: SakId): String {
        val arenaMigrering = arenaMigreringRepository.hentForSakHvisEksisterer(sakId)
        requireNotNull(arenaMigrering) {
            "Fant ingen arenamigrering for sak $sakId, kan ikke migrere lovvalg og medlemskap."
        }
        return arenaMigrering.saksnummerArena
    }

    fun hentLovvalgOgMedlemskapFraArena(sakId: SakId): ArenaLovvalgOgMedlemskapResponse {
        val saksnummerArena = hentSaksnummerArena(sakId)
        val lovvalgOgMedlemskapFraArena =
            apiInternGateway.hentLovvalgOgMedlemskapFraArena(saksnummerArena)

        // TODO lagre ned dataene for sporbarhet og etterprøving?

        requireNotNull(lovvalgOgMedlemskapFraArena) {
            "Fant ingen lovvalg og medlemskapsvurdering for sak $sakId ($saksnummerArena), kan ikke migrere lovvalg og medlemskap."
        }
        return lovvalgOgMedlemskapFraArena
    }
}
