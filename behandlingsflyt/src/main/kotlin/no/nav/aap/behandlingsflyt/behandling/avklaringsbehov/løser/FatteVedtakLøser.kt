package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.exception.KanIkkeVurdereEgneVurderingerException
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider

class FatteVedtakLøser(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val unleashGateway: UnleashGateway
) : AvklaringsbehovsLøser<FatteVedtakLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        gatewayProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: FatteVedtakLøsning
    ): LøsningsResultat {
        val avklaringsbehovene =
            avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId = kontekst.kontekst.behandlingId)

        val vurderingerSomErTotrinnskontrollert = løsning.vurderinger
            .filter { Definisjon.forKode(it.definisjon).kreverToTrinn }
            .filter { it.godkjent != null }
        vurderingerSomErTotrinnskontrollert.all { it.valider() }
        validerAvklaringsbehovOppMotBruker(avklaringsbehovene.alle().filter { it.erTotrinn() }, kontekst.bruker)

        vurderingerSomErTotrinnskontrollert.forEach { vurdering ->
            avklaringsbehovene.vurderTotrinn(
                definisjon = Definisjon.forKode(vurdering.definisjon),
                begrunnelse = vurdering.begrunnelse(),
                godkjent = vurdering.godkjent!!,
                årsakTilRetur = vurdering.grunner.orEmpty(),
                vurdertAv = kontekst.bruker.ident
            )
        }

        val sammenstiltBegrunnelse = sammenstillBegrunnelse(løsning)

        return LøsningsResultat(sammenstiltBegrunnelse)
    }

    private fun validerAvklaringsbehovOppMotBruker(avklaringsbehovene: List<Avklaringsbehov>, bruker: Bruker) {
        if (!unleashGateway.isEnabled(
                BehandlingsflytFeature.IngenValidering,
                bruker.ident
            ) && avklaringsbehovene.any { it.brukere().contains(bruker.ident) }
        ) {
            throw KanIkkeVurdereEgneVurderingerException()
        }
    }
    
    private fun sammenstillBegrunnelse(løsning: FatteVedtakLøsning): String {
        return løsning.vurderinger.joinToString("\\n") { it.begrunnelse() }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FATTE_VEDTAK
    }
}
