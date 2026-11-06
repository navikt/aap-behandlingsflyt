package no.nav.aap.behandlingsflyt.behandling.egenskaper

import no.nav.aap.behandlingsflyt.arena.ArenaOppslagGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

data class BehandlingEgenskaper(
    val erMigrertFraArena: Boolean,
    val harArenaHistorikk: Boolean,
)

/**
 * Egenskaper som kan utledes fra databasen. Hentes i en transaksjon, før eksterne kall gjøres
 * i [BehandlingEgenskaperService] utenfor transaksjonen.
 */
data class BehandlingEgenskaperGrunnlag(
    val ident: Ident,
    val erMigrertFraArena: Boolean,
)

class BehandlingEgenskaperGrunnlagService(
    private val kravRepository: KravRepository,
    private val sakRepository: SakRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        kravRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
    )

    fun hentGrunnlag(behandling: Behandling): BehandlingEgenskaperGrunnlag {
        return BehandlingEgenskaperGrunnlag(
            ident = sakRepository.hent(behandling.sakId).person.aktivIdent(),
            erMigrertFraArena = kravRepository.hentHvisEksisterer(behandling.id)?.gjeldendeMigrertKrav() != null,
        )
    }
}

class BehandlingEgenskaperService(
    private val arenaOppslagGateway: ArenaOppslagGateway,
) {
    constructor(gatewayProvider: GatewayProvider) : this(
        arenaOppslagGateway = gatewayProvider.provide(),
    )

    fun hentEgenskaper(grunnlag: BehandlingEgenskaperGrunnlag): BehandlingEgenskaper {
        return BehandlingEgenskaper(
            erMigrertFraArena = grunnlag.erMigrertFraArena,
            harArenaHistorikk = arenaOppslagGateway.hentHarHistorikk(grunnlag.ident).harHistorikk,
        )
    }
}
