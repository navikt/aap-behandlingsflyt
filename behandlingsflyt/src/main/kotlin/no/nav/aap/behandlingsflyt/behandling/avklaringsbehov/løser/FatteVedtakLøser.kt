package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.exception.KanIkkeVurdereEgneVurderingerException
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider

class FatteVedtakLøser(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlingRepository: BehandlingRepository,
    private val unleashGateway: UnleashGateway
) : AvklaringsbehovsLøser<FatteVedtakLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        gatewayProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: FatteVedtakLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val avklaringsbehovene =
            avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId = kontekst.kontekst.behandlingId)

        løsning.vurderinger.all { it.valider() }
        validerAvklaringsbehovOppMotBruker(avklaringsbehovene.alle().filter { it.erTotrinn() }, kontekst.bruker)

        if (skalSendesTilbake(løsning.vurderinger)) {
            val flyt = behandling.flyt()
            val vurderingerSomErSendtTilbake = løsning.vurderinger
                .filter { it.godkjent == false }

            val tidligsteStegMedRetur = vurderingerSomErSendtTilbake
                .map { Definisjon.forKode(it.definisjon) }
                .map { it.løsesISteg }
                .minWith(flyt.stegComparator)

            val vurderingerFørRetur = løsning.vurderinger
                .filter { it.godkjent == true }
                .filter {
                    flyt.erStegFør(
                        Definisjon.forKode(it.definisjon).løsesISteg,
                        tidligsteStegMedRetur
                    )
                }

            val unntaksVurderinger = listOf(
                Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP.kode,
                Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP.kode
            )

            val vurderingerSomMåReåpnes =
                if (unntaksVurderinger.containsAll(vurderingerSomErSendtTilbake.map { it.definisjon })) {
                    emptyList()
                } else {
                    avklaringsbehovene.alleEkskludertVentebehov()
                        .filterNot { behov ->
                            behov.definisjon in setOf(
                                Definisjon.FORESLÅ_VEDTAK,
                                Definisjon.FATTE_VEDTAK,
                                Definisjon.KVALITETSSIKRING
                            )
                        }
                        .filterNot { flyt.erStegFør(it.definisjon.løsesISteg, tidligsteStegMedRetur) }
                        .filter { vurdering ->
                            vurderingerSomErSendtTilbake.none { it.definisjon == vurdering.definisjon.kode } &&
                                    vurderingerFørRetur.none { it.definisjon == vurdering.definisjon.kode }
                        }
                }

            vurderingerFørRetur.forEach { vurdering ->
                avklaringsbehovene.vurderTotrinn(
                    definisjon = Definisjon.forKode(vurdering.definisjon),
                    godkjent = vurdering.godkjent!!,
                    begrunnelse = vurdering.begrunnelse(),
                    vurdertAv = kontekst.bruker.ident
                )
            }

            vurderingerSomErSendtTilbake.forEach { vurdering ->
                avklaringsbehovene.vurderTotrinn(
                    definisjon = Definisjon.forKode(vurdering.definisjon),
                    begrunnelse = vurdering.begrunnelse(),
                    godkjent = vurdering.godkjent!!,
                    årsakTilRetur = vurdering.grunner.orEmpty(),
                    vurdertAv = kontekst.bruker.ident
                )
            }

            vurderingerSomMåReåpnes.forEach { vurdering ->
                avklaringsbehovene.reåpne(definisjon = vurdering.definisjon)
            }
        } else {
            løsning.vurderinger.forEach { vurdering ->
                avklaringsbehovene.vurderTotrinn(
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

    private fun validerAvklaringsbehovOppMotBruker(avklaringsbehovene: List<Avklaringsbehov>, bruker: Bruker) {
        if (!unleashGateway.isEnabled(
                BehandlingsflytFeature.IngenValidering,
                bruker.ident
            ) && avklaringsbehovene.any { it.brukere().contains(bruker.ident) }
        ) {
            throw KanIkkeVurdereEgneVurderingerException()
        }
    }

    private fun skalSendesTilbake(vurderinger: List<TotrinnsVurdering>): Boolean {
        return vurderinger.any { it.godkjent == false }
    }

    private fun sammenstillBegrunnelse(løsning: FatteVedtakLøsning): String {
        return løsning.vurderinger.joinToString("\\n") { it.begrunnelse() }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FATTE_VEDTAK
    }
}
