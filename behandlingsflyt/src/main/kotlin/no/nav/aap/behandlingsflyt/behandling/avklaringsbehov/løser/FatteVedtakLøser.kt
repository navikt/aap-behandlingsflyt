package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.KanIkkeVurdereEgneVurderingerException
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.lookup.repository.RepositoryProvider

class FatteVedtakLøser(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlingRepository: BehandlingRepository,
) : AvklaringsbehovsLøser<FatteVedtakLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
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
            val flyt = utledType(behandling.typeBehandling()).flyt()
            val vurderingerSomErSendtTilbake = løsning.vurderinger
                .filter { it.godkjent == false }

            val tidligsteStegMedRetur = vurderingerSomErSendtTilbake
                .map { Definisjon.forKode(it.definisjon) }
                .map { it.løsesISteg }
                .minWith(flyt.compareable())

            val vurderingerFørRetur = løsning.vurderinger
                .filter { it.godkjent == true }
                .filter {
                    flyt.erStegFør(
                        Definisjon.forKode(it.definisjon).løsesISteg,
                        tidligsteStegMedRetur
                    )
                }

            val vurderingerSomMåReåpnes = avklaringsbehovene.alle()
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
                    årsakTilRetur = vurdering.grunner ?: listOf(),
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
        if (Miljø.er() == MiljøKode.PROD && avklaringsbehovene.any { it.brukere().contains(bruker.ident) }) {
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
