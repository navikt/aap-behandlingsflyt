package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOppfølgingLokalkontorLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOppfølgingNAYLøsning
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.KonsekvensAvOppfølging
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsBehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsoppgaveGrunnlagDto
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryProvider

/**
 * Har disse to (denne og [AvklarOppfølgingNAYLøser] i samme fil siden de deler logikk. Er to klasser kun fordi de løser
 * to forskjellige avklaringsbehobv.
 */
class AvklarOppfølgingLokalkontorLøser(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider,
) :
    AvklaringsbehovsLøser<AvklarOppfølgingLokalkontorLøsning> {
    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOppfølgingLokalkontorLøsning
    ): LøsningsResultat {
        val repo = repositoryProvider.provide<OppfølgingsBehandlingRepository>()

        return resultat(kontekst, repo, repositoryProvider, gatewayProvider, løsning.avklarOppfølgingsbehovVurdering)
    }

    override fun forBehov(): Definisjon = Definisjon.AVKLAR_OPPFØLGINGSBEHOV_LOKALKONTOR
}

class AvklarOppfølgingNAYLøser(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider,
) :
    AvklaringsbehovsLøser<AvklarOppfølgingNAYLøsning> {
    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOppfølgingNAYLøsning
    ): LøsningsResultat {
        val repo = repositoryProvider.provide<OppfølgingsBehandlingRepository>()

        return resultat(kontekst, repo, repositoryProvider, gatewayProvider, løsning.avklarOppfølgingsbehovVurdering)
    }

    override fun forBehov(): Definisjon = Definisjon.AVKLAR_OPPFØLGINGSBEHOV_NAY
}

private fun resultat(
    kontekst: AvklaringsbehovKontekst,
    oppfølgingsBehandlingsRepo: OppfølgingsBehandlingRepository,
    repositoryProvider: RepositoryProvider,
    gatewayProvider: GatewayProvider,
    løsning: OppfølgingsoppgaveGrunnlagDto
): LøsningsResultat {
    val behandlingId = kontekst.behandlingId()
    val vurdertAv = kontekst.bruker.ident

    if (løsning.konsekvensAvOppfølging == KonsekvensAvOppfølging.OPPRETT_VURDERINGSBEHOV) {
        if (løsning.opplysningerTilRevurdering.isNullOrEmpty()) {
            throw UgyldigForespørselException("Må oppgi opplysninger til revurdering.")
        }

        val behandlingService = BehandlingService(repositoryProvider, gatewayProvider)
        val trukketSøknadService = TrukketSøknadService(repositoryProvider)
        val ytelsesbehandling = behandlingService.finnSisteGjeldendeEllerÅpneYtelsesbehandling(kontekst.sakId())
        if (ytelsesbehandling != null && trukketSøknadService.søknadErTrukket(ytelsesbehandling.id)) {
            throw UgyldigForespørselException("Søknaden er trukket. Velg \"Ingen konsekvens for saken\" for å behandle oppfølgingsoppgaven.")
        }
    }

    oppfølgingsBehandlingsRepo.lagre(behandlingId, løsning.tilOppfølgingsoppgaveGrunnlag(vurdertAv))

    return LøsningsResultat("Lagret oppfølgingsbehov for $behandlingId med vurdert av $vurdertAv")
}