package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.exception.KanIkkeVurdereEgneVurderingerException
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KvalitetssikringLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider

class KvalitetssikrerLøser(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val unleashGateway: UnleashGateway
) : AvklaringsbehovsLøser<KvalitetssikringLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: KvalitetssikringLøsning
    ): LøsningsResultat {
        val avklaringsbehovene =
            avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId = kontekst.kontekst.behandlingId)

        val relevanteVurderinger =
            løsning.vurderinger.filter { Definisjon.forKode(it.definisjon).kvalitetssikres }
        relevanteVurderinger.all { it.valider() }

        validerAvklaringsbehovOppMotBruker(
            avklaringsbehovene.alle().filter { it.kreverKvalitetssikring() },
            kontekst.bruker
        )

        if (skalSendesTilbake(relevanteVurderinger)) {
            val vurderingerSomErSendtTilbake = relevanteVurderinger
                .filter { it.godkjent == false }

            val alle = relevanteVurderinger.filter { it.godkjent != null }

            alle.forEach { vurdering ->
                avklaringsbehovene.vurderKvalitet(
                    definisjon = Definisjon.forKode(vurdering.definisjon),
                    godkjent = vurdering.godkjent!!,
                    begrunnelse = vurdering.begrunnelse(),
                    vurdertAv = kontekst.bruker.ident,
                    årsakTilRetur = vurdering.grunner.orEmpty(),
                )
            }

            if (vurderingerSomErSendtTilbake.none { it.definisjon == Definisjon.SKRIV_SYKDOMSVURDERING_BREV.kode }) {
                avklaringsbehovene.vurderKvalitet(
                    definisjon = Definisjon.SKRIV_SYKDOMSVURDERING_BREV,
                    godkjent = false,
                    begrunnelse = "En tidligere vurdering ble ikke godkjent. Brev må skrives på nytt.",
                    vurdertAv = kontekst.bruker.ident
                )
            }
        } else {
            relevanteVurderinger.forEach { vurdering ->
                avklaringsbehovene.vurderKvalitet(
                    definisjon = Definisjon.forKode(vurdering.definisjon),
                    godkjent = vurdering.godkjent!!,
                    begrunnelse = vurdering.begrunnelse(),
                    vurdertAv = kontekst.bruker.ident
                )
            }
        }
        val sammenstiltBegrunnelse = sammenstillBegrunnelse(løsning)

        return LøsningsResultat(sammenstiltBegrunnelse)
    }

    private fun skalSendesTilbake(vurderinger: List<TotrinnsVurdering>): Boolean {
        return vurderinger.any { it.godkjent == false }
    }

    private fun sammenstillBegrunnelse(løsning: KvalitetssikringLøsning): String {
        return løsning.vurderinger.joinToString("\\n") { it.begrunnelse() }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.KVALITETSSIKRING
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
}
