package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KvalitetssikringLøsning
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class KvalitetssikrerLøser(
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) : AvklaringsbehovsLøser<KvalitetssikringLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: KvalitetssikringLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val avklaringsbehovene =
            avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId = kontekst.kontekst.behandlingId)

        val relevanteVurderinger =
            løsning.vurderinger.filter { Definisjon.forKode(it.definisjon).kvalitetssikres }
        relevanteVurderinger.all { it.valider() }

        if (skalSendesTilbake(relevanteVurderinger)) {
            val flyt = utledType(behandling.typeBehandling()).flyt()
            val vurderingerSomErSendtTilbake = relevanteVurderinger
                .filter { it.godkjent == false }

            val tidligsteStegMedRetur = vurderingerSomErSendtTilbake
                .map { Definisjon.forKode(it.definisjon) }
                .map { it.løsesISteg }
                .minWith(flyt.compareable())

            val vurderingerFørRetur = relevanteVurderinger
                .filter { it.godkjent == true }
                .filter {
                    flyt.erStegFør(
                        Definisjon.forKode(it.definisjon).løsesISteg,
                        tidligsteStegMedRetur
                    )
                }

            val vurderingerSomMåReåpnes = relevanteVurderinger
                .filter { vurdering ->
                    vurderingerSomErSendtTilbake.none { it.definisjon == vurdering.definisjon } &&
                            vurderingerFørRetur.none { it.definisjon == vurdering.definisjon }
                }

            vurderingerFørRetur.forEach { vurdering ->
                avklaringsbehovene.vurderKvalitet(
                    definisjon = Definisjon.forKode(vurdering.definisjon),
                    godkjent = vurdering.godkjent!!,
                    begrunnelse = vurdering.begrunnelse(),
                    vurdertAv = kontekst.bruker.ident
                )
            }

            vurderingerSomErSendtTilbake.forEach { vurdering ->
                avklaringsbehovene.vurderKvalitet(
                    definisjon = Definisjon.forKode(vurdering.definisjon),
                    begrunnelse = vurdering.begrunnelse(),
                    godkjent = vurdering.godkjent!!,
                    årsakTilRetur = vurdering.grunner ?: listOf(),
                    vurdertAv = kontekst.bruker.ident
                )
            }

            vurderingerSomMåReåpnes.forEach { vurdering ->
                avklaringsbehovene.reåpne(definisjon = Definisjon.forKode(vurdering.definisjon))
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
}
