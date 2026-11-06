package no.nav.aap.behandlingsflyt.behandling.egenskaper

import no.nav.aap.behandlingsflyt.arena.ArenaOppslagGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

data class BehandlingEgenskaper(
    val erMigrertFraArena: Boolean,
    val harArenaHistorikk: Boolean,
    val erKantIKantMedArena: Boolean,
)

/**
 * Egenskaper som kan utledes fra databasen. Hentes i en transaksjon, før eksterne kall gjøres
 * i [BehandlingEgenskaperService] utenfor transaksjonen.
 */
data class BehandlingEgenskaperGrunnlag(
    val ident: Ident,
    val erMigrertFraArena: Boolean,
    val erKantIKantMedArena: Boolean,
)

class BehandlingEgenskaperGrunnlagService(
    private val kravRepository: KravRepository,
    private val sakRepository: SakRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        kravRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        stønadsperiodeRepository = repositoryProvider.provide(),
    )

    fun hentGrunnlag(behandling: Behandling): BehandlingEgenskaperGrunnlag {
        val kravGrunnlag = kravRepository.hentHvisEksisterer(behandling.id)
        return BehandlingEgenskaperGrunnlag(
            ident = sakRepository.hent(behandling.sakId).person.aktivIdent(),
            erMigrertFraArena = kravGrunnlag?.gjeldendeMigrertKrav() != null,
            erKantIKantMedArena = erKantIKantMedArena(behandling, kravGrunnlag?.gjeldendeRelevanteKrav().orEmpty()),
        )
    }

    private fun erKantIKantMedArena(
        behandling: Behandling,
        gjeldendeRelevanteKrav: Collection<RelevantKrav>,
    ): Boolean {
        val gjeldendeKravreferanser = gjeldendeRelevanteKrav.map { it.referanse }.toSet()
        return stønadsperiodeRepository.hentHvisEksisterer(behandling.id)
            ?.gjeldendeVurderinger()
            .orEmpty()
            .any { it.referanse in gjeldendeKravreferanser && it.anvendtArenaMaksdato != null }
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
            erKantIKantMedArena = grunnlag.erKantIKantMedArena,
        )
    }
}
